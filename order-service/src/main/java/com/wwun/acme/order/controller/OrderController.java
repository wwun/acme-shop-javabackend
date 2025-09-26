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

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderResponseDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.mapper.OrderMapper;
import com.wwun.acme.order.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private OrderService orderService;
    private OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper){
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrdersByUser(@PathVariable UUID userId){
        return ResponseEntity.status(HttpStatus.OK).body(orderService.findAllByUserId(userId)
            .stream()
            .map(orderMapper::toResponseDTO)
            .toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(orderMapper.toResponseDTO(orderService.findById(id).get()));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderCreateRequestDTO orderCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponseDTO(orderService.save(orderCreateRequestDTO)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable UUID id, @Valid @RequestBody OrderUpdateRequestDTO orderUpdateRequestDTO){
        return ResponseEntity.status(HttpStatus.OK).body(orderMapper.toResponseDTO(orderService.update(id, orderUpdateRequestDTO).get()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> deleteOrder(@PathVariable UUID id){
        orderService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
