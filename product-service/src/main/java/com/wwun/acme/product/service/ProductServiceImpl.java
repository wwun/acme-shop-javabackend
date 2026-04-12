package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Category;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.ProductAlreadyExistsException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.metric.CacheMetrics;
import com.wwun.acme.product.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    //private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetrics cacheMetrics;
    
    public ProductServiceImpl(ProductRepository productRepository, CategoryService categoryService, ProductMapper productMapper, CacheMetrics cacheMetrics) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productMapper = productMapper;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    //@Cacheable(cacheNames = "productsAll", key = "'ALL'")
    public List<ProductResponseDTO> findAll() {
        List<ProductResponseDTO> dtos = productRepository.findAll().stream()
        .map(productMapper::toResponseDTO)
        .toList();

        dtos.forEach(dto -> System.out.println(
            "DTO -> id=" + dto.id()
            + ", name=" + dto.name()
            + ", description=" + dto.description()
            + ", price=" + dto.price()
            + ", category=" + dto.category()
        ));

        return dtos;
    }

    @Override
    //@Cacheable(cacheNames = "productById", key = "#id")
    public ProductResponseDTO findById(UUID id) {
        return productMapper.toResponseDTO(productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id)));
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
            // existing.setStock(productUpdateRequestDTO.getStock());
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
