package com.wwun.acme.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
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
import com.wwun.acme.product.dto.ProductUpdateRequestDTO;
import com.wwun.acme.product.entity.Product;
import com.wwun.acme.product.exception.ProductAlreadyExistsException;
import com.wwun.acme.product.exception.ProductNotFoundException;
import com.wwun.acme.product.mapper.ProductMapper;
import com.wwun.acme.product.service.ProductService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(ProductController.class)
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

        //when(productService.findById(productId)).thenReturn(product);

        ProductResponseDTO productResponseDTO = new ProductResponseDTO(productId, "purifier", null, new BigDecimal("10.00"), null);

        when(productService.findById(productId)).thenReturn(productResponseDTO);
        
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
            .andExpect(jsonPath("$.errorMessage").value(containsString(productId.toString())));
            
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
        
        UUID productId = UUID.randomUUID();

        Product product = new Product();
        product.setId(productId);
        when(productService.save(any(ProductCreateRequestDTO.class))).thenReturn(product);

        ProductResponseDTO productResponseDTO = new ProductResponseDTO(productId, "purifier", null, new BigDecimal("10.00"), null);
        
        when(productMapper.toResponseDTO(product)).thenReturn(productResponseDTO);

        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productCreateRequestDTO))
            .with(csrf()))
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
        
        mockMvc.perform(post("/api/products")
            .content(objectMapper.writeValueAsString(productCreateRequestDTO))
            .contentType(MediaType.APPLICATION_JSON)
            .with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(400));

        verify(productService, never()).save(any(ProductCreateRequestDTO.class));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn409_whenProductNameAlreadyExists() throws Exception {
        // Given
        UUID categoryId = UUID.randomUUID();
 
        ProductCreateRequestDTO request = new ProductCreateRequestDTO();
        request.setCategoryId(categoryId);
        request.setPrice(new BigDecimal("10.00"));
        request.setName("vacuum");
        
        when(productService.save(any(ProductCreateRequestDTO.class)))
                .thenThrow(new ProductAlreadyExistsException("Name product already exists: vacuum"));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value(containsString("vacuum")));
 
        verify(productService).save(any(ProductCreateRequestDTO.class));
    
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn404_whenProductDoesNotExist() throws Exception {
        // Given
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
 
        ProductUpdateRequestDTO request = new ProductUpdateRequestDTO();
        request.setCategoryId(categoryId);
        request.setName("Ghost");
        request.setPrice(new BigDecimal("50.00"));
        
        when(productService.update(eq(productId), any(ProductUpdateRequestDTO.class)))
                .thenThrow(new ProductNotFoundException("Product not found: " + productId));
 
        // When / Then
        mockMvc.perform(put("/api/products/{id}", productId)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value(containsString(productId.toString())));
 
        verify(productService).update(eq(productId), any(ProductUpdateRequestDTO.class));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturn200_whenDeleteSucceeds() throws Exception{
        
        UUID productId = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", productId)
            .with(csrf()))
            .andExpect(status().isOk());

        verify(productService).delete(productId);
            
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturn404_whenProductDoesNotExist() throws Exception{

        UUID productId = UUID.randomUUID();

        doThrow(new ProductNotFoundException("Product not found with id: " + productId)).when(productService).delete(productId);

        mockMvc.perform(delete("/api/products/{id}", productId)
            .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statusCode").value(404))
            .andExpect(jsonPath("$.errorMessage").value(containsString(productId.toString())));

        verify(productService).delete(productId);

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn200_whenValidRequest() throws Exception{
        
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ProductUpdateRequestDTO productUpdateRequestDTO = new ProductUpdateRequestDTO();
        productUpdateRequestDTO.setCategoryId(categoryId);
        productUpdateRequestDTO.setName("Chanel");
        productUpdateRequestDTO.setPrice(new BigDecimal("339.00"));
        productUpdateRequestDTO.setDescription("100ml");

        Product product = new Product();
        product.setId(productId);

        when(productService.update(eq(productId), any(ProductUpdateRequestDTO.class))).thenReturn(Optional.of(product));

        ProductResponseDTO productResponseDTO = new ProductResponseDTO(productId, "purifier", null, new BigDecimal("10.00"), null);
        
        when(productMapper.toResponseDTO(product)).thenReturn(productResponseDTO);

        mockMvc.perform(put("/api/products/{id}", productId)
            .content(objectMapper.writeValueAsString(productUpdateRequestDTO))
            .contentType(MediaType.APPLICATION_JSON)
            .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(productId.toString()));

        verify(productService).update(eq(productId), any(ProductUpdateRequestDTO.class));
        verify(productMapper).toResponseDTO(product);

    }
    
}
