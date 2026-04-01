package com.wwun.acme.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.mapper.OrderItemMapper;
import com.wwun.acme.order.service.OrderItemService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orderItems/{orderId}/items")
public class OrderItemController {

    private OrderItemService orderItemService;
    private OrderItemMapper orderItemMapper;

    public OrderItemController(OrderItemService orderItemService, OrderItemMapper orderItemMapper){
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
    }

    @GetMapping //("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<OrderItemResponseDTO>> findAllByOrderId(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(orderItemService.findAllByOrderId(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<OrderItemResponseDTO> createOrderItem(@PathVariable UUID orderId, @Valid @RequestBody OrderItemCreateRequestDTO orderItemCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemMapper.toResponseDTO(orderItemService.save(orderId, orderItemCreateRequestDTO)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<OrderItemResponseDTO> updateOrderItem(@PathVariable UUID orderId, @PathVariable UUID id, @Valid @RequestBody OrderItemUpdateRequestDTO orderItemUpdateRequestDTO){
        return ResponseEntity.status(HttpStatus.OK).body(orderItemMapper.toResponseDTO(orderItemService.update(id, orderItemUpdateRequestDTO).get()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteOrderItem(@PathVariable UUID orderId, @PathVariable UUID id){
        orderItemService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
