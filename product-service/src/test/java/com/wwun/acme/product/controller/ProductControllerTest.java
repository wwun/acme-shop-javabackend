package com.wwun.acme.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.containsString;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.product.dto.ProductCreateRequestDTO;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.service.ProductService;

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
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn201_whenValidRequest() throws Exception{

        UUID categoryId = UUID.randomUUID();

        ProductCreateRequestDTO productCreateRequestDTO = new ProductCreateRequestDTO();
        productCreateRequestDTO.setCategoryId(categoryId);
        productCreateRequestDTO.setPrice(new BigDecimal("10.00"));
        productCreateRequestDTO.setName("purifier");
        productCreateRequestDTO.setStock(20);

        UUID productId = UUID.randomUUID();

        Product product = new Product();
        product.setId(productId);
        when(productService.save(any(ProductCreateRequestDTO.class))).thenReturn(product);

        ProductResponseDTO productResponseDTO = new ProductResponseDTO();
        productResponseDTO.setId(productId);
        productResponseDTO.setPrice(new BigDecimal("10.00"));
        productResponseDTO.setStock(20);

        when(productMapper.toResponseDTO(product)).thenReturn(productResponseDTO);

        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productCreateRequestDTO)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(productId.toString()))
            .andExpect(jsonPath("$.stock").value(20));

        verify(productService).save(any(ProductCreateRequestDTO.class));
        verify(productMapper).toResponseDTO(product);

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn400_whenInvalidRequest() throws Exception{

        ProductCreateRequestDTO productCreateRequestDTO = new ProductCreateRequestDTO();
        productCreateRequestDTO.setPrice(new BigDecimal("20.00"));
        productCreateRequestDTO.setStock(10);

        mockMvc.perform(post("/api/products")
            .content(objectMapper.writeValueAsString(productCreateRequestDTO))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(400));

        verify(productService, never()).save(any(ProductCreateRequestDTO.class));

    }

    void deleteProduct_shouldReturn200_whenDeleteSucceeds(){
        
    }
    
    // deleteProduct_shouldReturn404_whenProductDoesNotExist
    // updateProduct_shouldReturn200_whenValidRequest
    // updateProduct_shouldReturn400_whenInvalidRequest

}
