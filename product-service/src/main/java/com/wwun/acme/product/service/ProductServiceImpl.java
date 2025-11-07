package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.entity.StockMovement;
import com.wwun.acme.product.exception.CategoryNotFoundException;
import com.wwun.acme.product.exception.InsufficientStockException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.metric.CacheMetrics;
import com.wwun.acme.product.repository.CategoryRepository;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

import com.wwun.acme.product.enums.StockOperation;

@Service
public class ProductServiceImpl implements ProductService{

    //private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    // repository which contains use cases for CRUD operations

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;
    //private final RedisTemplate<String, Object> redisTemplate;
    
    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, StockMovementRepository stockMovementRepository, ProductMapper productMapper, CacheMetrics cacheMetrics) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Cacheable(cacheNames = "productsAll", key = "'ALL'")
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Cacheable(cacheNames = "productById", key = "#id")
    public Optional<Product> findById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional  //preguntar por propagation y isolation
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Product save(ProductCreateRequestDTO productCreateRequestDTO) {
        Category category = categoryRepository.findById(productCreateRequestDTO.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        Product product = productMapper.toEntity(productCreateRequestDTO);
        product.setCategory(category);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Optional<Product> update(UUID productId, ProductUpdateRequestDTO productUpdateRequestDTO) {
        return productRepository.findById(productId).map(existing -> {
            existing.setName(productUpdateRequestDTO.getName());
            existing.setPrice(productUpdateRequestDTO.getPrice());
            existing.setStock(productUpdateRequestDTO.getStock());
            existing.setCategory(categoryRepository.findById(productUpdateRequestDTO.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found")));
            return productRepository.save(existing);
        });
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public void delete(UUID productId) {
        if (!productRepository.existsById(productId))
            throw new ProductNotFoundException("Product not found with id: " + productId);

        productRepository.deleteById(productId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productById", "productsAll"}, allEntries = true)
    public Optional<Product> updateStock(UUID id, int amount) {
        if (amount < 0)
            throw new InvalidStockAmountException("Stock amount cannot be negative");

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        product.setStock(amount);
        Product updated = productRepository.save(product);
        
        stockMovementRepository.save(
            new StockMovement(LocalDateTime.now(), amount, StockOperation.SET, updated)
        );

        return Optional.of(updated);
    }


    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productById", "productsAll"}, allEntries = true)
    public Optional<Product> increaseStock(UUID id, int amount) {
        if (amount <= 0)
            throw new InvalidStockAmountException("Increase amount must be positive");

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        product.setStock(product.getStock() == null ? amount : product.getStock() + amount);
        Product updated = productRepository.save(product);

        stockMovementRepository.save(
            new StockMovement(LocalDateTime.now(), amount, StockOperation.INCREASE, updated)
        );

        return Optional.of(updated);
    }


    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productById", "productsAll"}, allEntries = true)
    public Optional<Product> decreaseStock(UUID id, int amount) {
        if (amount <= 0)
            throw new InvalidStockAmountException("Decrease amount must be positive");

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        if (product.getStock() == null || product.getStock() < amount)
            throw new InsufficientStockException("Insufficient stock: " + id);

        product.setStock(product.getStock() - amount);
        Product updated = productRepository.save(product);

        stockMovementRepository.save(
            new StockMovement(LocalDateTime.now(), amount, StockOperation.DECREASE, updated)
        );

        return Optional.of(updated);
    }

    @Override
    public BigDecimal getProductPrice(UUID id){
        return productRepository.findById(id).map(Product::getPrice).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<ProductResponseDTO> getAllById(List<UUID> productsId){
        return productRepository.findAllById(productsId).stream()
        .map(productMapper::toResponseDTO)
        .collect(Collectors.toList());
    }

    private boolean isValidProduct(Product product){

        boolean isValid = true;

        if(product.getName()==null || product.getName().isBlank())
            isValid = false;

        if(product.getPrice() != null && product.getPrice().signum() < 0){
            isValid = false;
        }

        if(product.getStock()!= null && product.getStock() < 0){
            isValid = false;
        }

        return isValid;
    }
}
