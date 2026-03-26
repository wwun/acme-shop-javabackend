package com.wwun.acme.product.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.containsString;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.service.ProductService;

import jakarta.validation.Valid;

@WebMvcTest(ProductControllerTest.class)
@Import(GlobalExceptionHandler.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductMapper productMapper;

    @MockBean
    ProductService productService;

    @Test
    @WithMockUser(roles = "USER")
    void getProductById_shouldReturn200_whenProductExists() throws Exception{

        //Given
        UUID productId = UUID.randomUUID();
        
        Product product = new Product();
        product.setId(productId);

        when(productService.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO productResponseDTO = new ProductResponseDTO();
        productResponseDTO.setId(productId);

        when(productMapper.toResponseDTO(product)).thenReturn(productResponseDTO);
        
        mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.id").value(productId.toString()));

        verify(productService).findById(productId);
        verify(productMapper).toResponseDTO(product);

    }

    @Test
    @WithMockUser(roles = "USER")
    void getProductById_shouldReturn404_whenProductDoesNotExist() throws Exception{

        UUID productId = UUID.randomUUID();

        when(productService.findById(productId)).thenThrow(new ProductNotFoundException("Product not found with id: " +productId));

        mockMvc.perform(get("/api/products/{id}", productId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value(containsString(productId.toString())));
            
            verify(productService).findById(productId);

    }
    
    @Test
    @WithMockUser("ADMIN")
    void createProduct_shouldReturn201_whenValidRequest(){

        // @Valid @RequestBody ProductCreateRequestDTO productCreateRequestDTO){
        // Product product = productService.save(productCreateRequestDTO);
        // return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toResponseDTO(product

        

    }
    
    // createProduct_shouldReturn400_whenInvalidRequest
    // deleteProduct_shouldReturn200_whenDeleteSucceeds
    // deleteProduct_shouldReturn404_whenProductDoesNotExist
    // updateProduct_shouldReturn200_whenValidRequest
    // updateProduct_shouldReturn400_whenInvalidRequest

}
