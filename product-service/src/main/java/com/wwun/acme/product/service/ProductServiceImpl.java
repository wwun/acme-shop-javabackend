package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.wwun.acme.product.exception.InsufficientStockException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductAlreadyExistsException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.metric.CacheMetrics;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

import com.wwun.acme.product.enums.StockOperation;

@Service
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;
    //private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetrics cacheMetrics;
    
    public ProductServiceImpl(ProductRepository productRepository, CategoryService categoryService, StockMovementRepository stockMovementRepository, ProductMapper productMapper, CacheMetrics cacheMetrics) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.stockMovementRepository = stockMovementRepository;
        this.productMapper = productMapper;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    @Cacheable(cacheNames = "productsAll", key = "'ALL'")
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Cacheable(cacheNames = "productById", key = "#id")
    public Product findById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Product save(ProductCreateRequestDTO productCreateRequestDTO) {
        Category category = categoryService.findById(productCreateRequestDTO.getCategoryId());
        Product product = productMapper.toEntity(productCreateRequestDTO);
        product.setCategory(category);

        if(productRepository.findByName(product.getName()).isPresent())
            throw new ProductAlreadyExistsException("Name product already exists: " + product.getName());

        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Optional<Product> update(UUID productId, ProductUpdateRequestDTO productUpdateRequestDTO) {
        productRepository.findByName(productUpdateRequestDTO.getName())
            .ifPresent(existing -> {
                if (!existing.getId().equals(productId))
                    throw new ProductAlreadyExistsException("Product name already exists");
            });

        return productRepository.findById(productId).map(existing -> {
            existing.setName(productUpdateRequestDTO.getName());
            existing.setPrice(productUpdateRequestDTO.getPrice());
            existing.setStock(productUpdateRequestDTO.getStock());
            existing.setCategory(categoryService.findById(productUpdateRequestDTO.getCategoryId()));
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
        
        stockMovementRepository.save(StockMovement.builder()
            .createdAt(Instant.now())
            .quantity(amount)
            .operation(StockOperation.SET)
            //.product(updated)
            .build());

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

        stockMovementRepository.save(StockMovement.builder()
            .createdAt(Instant.now())
            .quantity(amount)
            .operation(StockOperation.INCREASE)
            //.product(updated)
            .build()
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

        stockMovementRepository.save(StockMovement.builder()
            .createdAt(Instant.now())
            .quantity(amount)
            .operation(StockOperation.DECREASE)
            //.product(updated)
            .build()
        );

        return Optional.of(updated);
    }

    @Override
    public BigDecimal getProductPrice(UUID id){
        return productRepository.findById(id).map(Product::getPrice).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<ProductResponseDTO> getAllById(List<UUID> productsId){
        List<Product> found = productRepository.findAllById(productsId);
        if (found.size() != productsId.size())
            throw new ProductNotFoundException("One or more products not found");
        return found.stream().map(productMapper::toResponseDTO).toList();
    }

}
