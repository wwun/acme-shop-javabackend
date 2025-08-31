package com.wwun.acme.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.wwun.acme.product.exception.InvalidProductException;
import com.wwun.acme.product.exception.InvalidStockAmountException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.repository.CategoryRepository;
import com.wwun.acme.product.repository.ProductRepository;
import com.wwun.acme.product.repository.StockMovementRepository;

import com.wwun.acme.product.enums.StockOperation;

@Service
public class ProductServiceImpl implements ProductService{

    // repository which contains use cases for CRUD operations

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, StockMovementRepository stockMovementRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productMapper = productMapper;
    }

    @Override
    public List<Product> findAll() {
        return (List<Product>)productRepository.findAll();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional  //preguntar por propagation y isolation
    public Product save(ProductCreateRequestDTO productCreateRequestDTO) {
        Category category = categoryRepository.findById(productCreateRequestDTO.getCategoryId()).orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        Product product = productMapper.toEntity(productCreateRequestDTO);
        if(!isValidProduct(product)){
            throw new InvalidProductException("Invalid product data in DTO");
        }
        product.setCategory(category);
        return productRepository.save(product);
    }

    @Transactional
    private Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Optional<Product> update(UUID id, ProductUpdateRequestDTO productUpdateRequestDTO) {

        if(productRepository.findById(id).isEmpty()){
            throw new ProductNotFoundException("Product not found with id: " + id);
        }

        Product product = productMapper.toEntity(productUpdateRequestDTO);
        if(!isValidProduct(product)){
            throw new InvalidProductException("Invalid product data in DTO");
        }

        return productRepository.findById(id)
        .map(existing -> {
            existing.setName(product.getName());
            existing.setPrice(product.getPrice());
            existing.setStock(product.getStock());

            Category category = null;

            if(existing.getCategory().getId()!=productUpdateRequestDTO.getCategoryId()){
                category = categoryRepository.findById(productUpdateRequestDTO.getCategoryId()).orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            }else{
                category = categoryRepository.findById(existing.getCategory().getId()).orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            }
            
            existing.setCategory(category);

            return productRepository.save(existing);
        });
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        if(productRepository.findById(id).isEmpty())
            throw new ProductNotFoundException("Product not found with id: " + id);

        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Product> updateStock(UUID id, int amount) { //if the amount want to be set directly

        if(amount < 0) {
            throw new InvalidStockAmountException("Stock ammount cannot be negative");
        }
        
        if(!productRepository.existsById(id)){
            throw new ProductNotFoundException("Product not found with id: " + id);
        }            

        return productRepository.findById(id)
        .map(product -> {
            if(product.getStock() == null){
                product.setStock(0);
            }
            product.setStock(amount);
            Product p = productRepository.save(product);
            if(p!=null){
                StockMovement stockMovement = new StockMovement(LocalDateTime.now(), amount, StockOperation.SET, p);
                stockMovementRepository.save(stockMovement);
            }
            return p;
        });
    }

    @Override
    @Transactional
    public Optional<Product> increaseStock(UUID id, int amount){
        
        if(amount <= 0) {
            throw new InvalidStockAmountException("Stock ammount cannot be zero or negative");
        }
        
        if(!productRepository.existsById(id)){
            throw new ProductNotFoundException("Product not found with id: " + id);
        }

        return productRepository.findById(id)
        .map(product -> {
            if(product.getStock() == null){
                product.setStock(0);
            }
            product.setStock(product.getStock()+amount);
            Product p = productRepository.save(product);
            if(p!=null){
                StockMovement stockMovement = new StockMovement(LocalDateTime.now(), amount, StockOperation.DECREASE, p);
                stockMovementRepository.save(stockMovement);
            }
            return p;
        });
    }

    @Override
    @Transactional
    public Optional<Product> decreaseStock(UUID id, int amount) {

        if(amount <= 0) {
            throw new InvalidStockAmountException("Stock ammount cannot be zero or negative, this value will be decreased from the current stock");
        }
        
        if(!productRepository.existsById(id)){
            throw new ProductNotFoundException("Product not found with id: " + id);
        }

        return productRepository.findById(id)
        .flatMap(product -> {
            if (product.getStock() == null || product.getStock() < amount) {
                throw new InsufficientStockException("Insufficient stock for product with id: " + id);
            } else {
                product.setStock(product.getStock() - amount);
                Product p = productRepository.save(product);
                if(p!=null){
                    StockMovement stockMovement = new StockMovement(LocalDateTime.now(), amount, StockOperation.INCREASE, p);
                    stockMovementRepository.save(stockMovement);
                }
                return Optional.of(p);
            }
        });
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
