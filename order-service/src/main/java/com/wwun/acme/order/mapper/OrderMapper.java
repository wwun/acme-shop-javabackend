package com.wwun.acme.order.mapper;

import org.mapstruct.Mapper;

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderResponseDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.entity.Order;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    Order toEntity(OrderCreateRequestDTO orderCreateRequestDTO);
    Order toEntity(OrderUpdateRequestDTO orderUpdateRequestDTO);
    OrderResponseDTO toResponseDTO(Order order);

}
