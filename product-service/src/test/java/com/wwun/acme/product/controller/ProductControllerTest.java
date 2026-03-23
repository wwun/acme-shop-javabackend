package com.wwun.acme.product.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.product.dto.ProductResponseDTO;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.service.ProductService;
import com.wwun.acme.product.service.ProductServiceImpl;

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

    // getProductById_shouldReturn404_whenProductDoesNotExist
    // createProduct_shouldReturn201_whenValidRequest
    // createProduct_shouldReturn400_whenInvalidRequest
    // deleteProduct_shouldReturn200_whenDeleteSucceeds
    // deleteProduct_shouldReturn404_whenProductDoesNotExist
    // updateProduct_shouldReturn200_whenValidRequest
    // updateProduct_shouldReturn400_whenInvalidRequest

}
