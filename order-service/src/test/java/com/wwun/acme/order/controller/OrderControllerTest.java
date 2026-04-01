package com.wwun.acme.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.exception.OrderNotFoundException;
import com.wwun.acme.order.mapper.OrderMapper;
import com.wwun.acme.order.service.OrderService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderMapper orderMapper;

    @Test
    @WithMockUser(roles = "USER")
    void getOrderById_shouldReturn200AndOrderResponse_whenOrderExists() throws Exception {

        // Given
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);

        OrderResponseDTO dto = new OrderResponseDTO(orderId, UUID.randomUUID(), Instant.now(), new BigDecimal("25.50"), List.of());

        when(orderService.findById(orderId)).thenReturn(order);
        when(orderMapper.toResponseDTO(order)).thenReturn(dto);

        // When + Then
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getOrderById_shouldReturn404_whenOrderNotFound() throws Exception{
        
        //Given
        UUID orderId = UUID.randomUUID();

        when(orderService.findById(orderId)).thenThrow(new OrderNotFoundException("Order not found"));
        
        mockMvc.perform(get("/api/orders/{id}", orderId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteOrder_shouldReturn200_whenDeleteSucceeds() throws Exception{

        UUID orderId = UUID.randomUUID();

        mockMvc.perform(delete("/api/orders/{id}", orderId)
            .with(csrf()))
            .andExpect(status().isOk());

        verify(orderService).delete(orderId);

    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturn201_whenValidRequest() throws Exception{

        //Given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();

        OrderItemCreateRequestDTO orderItemCreateRequestDTO = new OrderItemCreateRequestDTO();
        orderItemCreateRequestDTO.setProductId(productId);
        orderItemCreateRequestDTO.setQuantity(2);

        OrderCreateRequestDTO orderCreateRequestDTO = new OrderCreateRequestDTO();
        orderCreateRequestDTO.setItems(List.of(orderItemCreateRequestDTO));
        
        Order order = new Order();
        order.setId(orderId);
        
        when(orderService.save(eq(idempotencyKey), any(OrderCreateRequestDTO.class))).thenReturn(order);

        OrderResponseDTO orderResponseDTO = new OrderResponseDTO(orderId, UUID.randomUUID(), Instant.now(), new BigDecimal("20.00"), List.of());

        when(orderService.save(eq(idempotencyKey), any(OrderCreateRequestDTO.class))).thenReturn(order);
        when(orderMapper.toResponseDTO(order)).thenReturn(orderResponseDTO);

        mockMvc.perform(post("/api/orders")
            .header("Idempotency-Key", idempotencyKey.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(orderCreateRequestDTO))
            .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(orderId.toString()));

    }

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_shouldReturn400_whenInvalidRequest() throws Exception {

        // Given
        UUID idempotencyKey = UUID.randomUUID();

        OrderCreateRequestDTO invalidRequest = new OrderCreateRequestDTO();
        
        mockMvc.perform(post("/api/orders")
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400));

        verify(orderService, never()).save(eq(idempotencyKey), any(OrderCreateRequestDTO.class));

    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllOrdersByUser_shouldReturn200_whenUserHasOrders() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
 
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
 
        OrderResponseDTO dto = new OrderResponseDTO(orderId, userId, Instant.now(), new BigDecimal("50.00"), List.of());
 
        when(orderService.findAllByUserId(userId)).thenReturn(List.of(order));
        when(orderMapper.toResponseDTO(order)).thenReturn(dto);
 
        // When / Then
        mockMvc.perform(get("/api/orders/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
 
        verify(orderService).findAllByUserId(userId);
    
    }

}
