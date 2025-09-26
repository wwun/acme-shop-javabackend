package com.wwun.acme.product.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.enums.StockOperation;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper){
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts(){
        return ResponseEntity.status(HttpStatus.OK).body(productService.findAll()
            .stream()
            .map(productMapper::toResponseDTO)  //map(product -> productMapper.toResponseDTO(product))
            .toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable UUID id){
        return productService.findById(id)
            .map(product -> productMapper.toResponseDTO(product))
            .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " +id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductCreateRequestDTO productCreateRequestDTO){
        Product product = productService.save(productCreateRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponseDTO(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequestDTO productUpdateRequestDTO){
        Optional<Product> updatedProduct = productService.update(id, productUpdateRequestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(productMapper.toResponseDTO(updatedProduct.get()));        
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id){
        productService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
//B4$3d3d4t0$

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/{stock}/{operation}")
    public ResponseEntity<?> updateProductStock(@PathVariable UUID id, @PathVariable int stock, @PathVariable StockOperation operation ){ //@RequestParam http://example.com/users?name=Juan&age=30 @RequestParam("age") int age
        Optional<Product> product;
        
        switch(operation){
            case INCREASE:
                product = productService.increaseStock(id, stock);
                break;
            case DECREASE:
                product = productService.decreaseStock(id, stock);
                break;
            case SET:
                product = productService.updateStock(id, stock);
                break;
            default:
                throw new IllegalArgumentException("Invalid stock operation: " + operation);
        }
        
        return ResponseEntity.status(HttpStatus.OK).body(productMapper.toResponseDTO(product.get()));
                
    }

    //

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}/price")
    public ResponseEntity<?> getProductPrice(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductPrice(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/listids")
    public ResponseEntity<List<ProductResponseDTO>> getAllById(@RequestBody List<UUID> productsId){
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllById(productsId));
    }
}
