package com.wwun.acme.order.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItem toEntity(OrderItemCreateRequestDTO orderCreateRequestDTO);
    OrderItem toEntity(OrderItemUpdateRequestDTO orderUpdateRequestDTO);
    OrderItemResponseDTO toResponseDTO(OrderItem orderItem);


}
