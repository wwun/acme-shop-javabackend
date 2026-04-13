package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private final static Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    
    public ProductServiceImpl(ProductRepository productRepository, CategoryService categoryService, ProductMapper productMapper, CacheMetrics cacheMetrics) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productMapper = productMapper;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    @Cacheable(cacheNames = "productsAll", key = "'ALL'")
    public List<ProductResponseDTO> findAll() {
        log.info("finding all products");
        List<ProductResponseDTO> dtos = productRepository.findAll().stream().map(productMapper::toResponseDTO).toList();

        return dtos;
    }

    @Override
    @Cacheable(cacheNames = "productById", key = "#id")
    public ProductResponseDTO findById(UUID id) {
        if(id==null){
            log.warn("findById a product called with id null");
            throw new IllegalArgumentException("id cannot be null");
        }
        log.info("Searching product by id");
        return productMapper.toResponseDTO(productRepository.findById(id).orElseThrow(() -> {
            log.error("Product not found with id: ", id);
            return new ProductNotFoundException("Product not found with id: " + id);
        }));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Product save(ProductCreateRequestDTO productCreateRequestDTO) {
        if(productCreateRequestDTO==null){
            log.warn("request dto to create product cannot be null");
            throw new IllegalArgumentException("productCreateRequestDTO cannot be null");
        }

        UUID categoryId = productCreateRequestDTO.getCategoryId();

        if(categoryId==null){
            log.warn("save product called with category id null");
            throw new IllegalArgumentException("categoryId cannot be null");
        }
        
        Category category = categoryService.findById(categoryId);
        Product product = productMapper.toEntity(productCreateRequestDTO);
        product.setCategory(category);

        if(productRepository.findByName(product.getName()).isPresent()){
            log.error("Name product already exists: ", product.getName());
            throw new ProductAlreadyExistsException("Name product already exists: " + product.getName());
        }

        log.info("Saving product initiated");

        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public Optional<Product> update(UUID productId, ProductUpdateRequestDTO productUpdateRequestDTO) {
        if(productUpdateRequestDTO==null){
            log.warn("request dto to update product cannot be null");
            throw new IllegalArgumentException("productUpdateRequestDTO cannot be null");
        }

        productRepository.findByName(productUpdateRequestDTO.getName())
            .ifPresent(existing -> {
                if (!existing.getId().equals(productId)){
                    log.error("Product name already exists");
                    throw new ProductAlreadyExistsException("Product name already exists");
                }
            });

        log.info("Updating product initiated");

        return productRepository.findById(productId).map(existing -> {
            existing.setName(productUpdateRequestDTO.getName());
            existing.setPrice(productUpdateRequestDTO.getPrice());
            existing.setCategory(categoryService.findById(productUpdateRequestDTO.getCategoryId()));
            return productRepository.save(existing);
        });
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"productsAll", "productById"}, allEntries = true)
    public void delete(UUID productId) {
        if(productId==null){
            log.warn("delete a product called with id null");
            throw new IllegalArgumentException("id cannot be null");
        }

        if (!productRepository.existsById(productId)){
            log.error("Product not found with id: ", productId);
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        log.info("Deleting product initiated");

        productRepository.deleteById(productId);
    }

    @Override
    public BigDecimal getProductPrice(UUID id){
        if(id==null){
            log.warn("findById a product called with id null");
            throw new IllegalArgumentException("id cannot be null");
        }

        log.info("getProductPrice initiated");

        return productRepository.findById(id).map(Product::getPrice).orElseThrow(() -> {
            log.error("Product not found with id: ", id);
            return new ProductNotFoundException("Product not found with id: " + id);
        });
    }

    @Override
    public List<ProductResponseDTO> getAllById(List<UUID> productsId){
        List<Product> found = productRepository.findAllById(productsId);
        if (found.size() != productsId.size()){
            log.error("One or more products not found");            
            throw new ProductNotFoundException("One or more products not found");
        }
        return found.stream().map(productMapper::toResponseDTO).toList();
    }

}
