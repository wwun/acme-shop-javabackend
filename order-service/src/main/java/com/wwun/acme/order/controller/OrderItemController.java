package com.wwun.acme.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/orderItems")
public class OrderItemController {

    private OrderItemService orderItemService;
    private OrderItemMapper orderItemMapper;

    public OrderItemController(OrderItemService orderItemService, OrderItemMapper orderItemMapper){
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
    }

    //no voy a agregar este metodo, entiendo qe si se qieren listar todos los orderItem de una order deberia ser por la misma order y no tiene sentido solicitar todas las items existentes en la bd
    // @GetMapping("/{id}")
    // public ResponseEntity<List<OrderResponseDTO>> getAll(@PathVariable UUID id){
    //     return ResponseEntity.status(HttpStatus.OK).body(orderItemService.findAll()
    //         .stream()
    //         .map(orderItemMapper::toResponseDTO)
    //         .toList());
    // }

    // @GetMapping("/{id}")
    // public ResponseEntity<OrderItemResponseDTO> getOrderItemById(@PathVariable UUID id){
    //     return ResponseEntity.status(HttpStatus.OK).body(orderItemMapper.toResponseDTO(orderItemService.findById(id).get()));
    // }

    @GetMapping("/{id}")
    public ResponseEntity<List<OrderItemResponseDTO>> findAllByOrderId(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(orderItemService.findAllByOrderId(id));
    }
    
    @PostMapping
    public ResponseEntity<OrderItemResponseDTO> createOrderItem(@Valid @RequestBody OrderItemCreateRequestDTO orderItemCreateRequestDTO){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderItemMapper.toResponseDTO(orderItemService.save(orderItemCreateRequestDTO)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderItemResponseDTO> updateOrderItem(@PathVariable UUID id, @Valid @RequestBody OrderItemUpdateRequestDTO orderItemUpdateRequestDTO){
        return ResponseEntity.status(HttpStatus.OK).body(orderItemMapper.toResponseDTO(orderItemService.update(id, orderItemUpdateRequestDTO).get()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrderItem(@PathVariable UUID id){
        orderItemService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
