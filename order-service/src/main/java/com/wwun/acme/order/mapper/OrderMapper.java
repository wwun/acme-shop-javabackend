package com.wwun.acme.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderResponseDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.entity.Order;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mappings({@Mapping(target = "id", ignore = true),
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "orderDate", ignore = true),
            @Mapping(target = "total", ignore = true)})
    Order toEntity(OrderCreateRequestDTO orderCreateRequestDTO);

    @Mappings({@Mapping(target = "id", ignore = true),
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "orderDate", ignore = true),
            @Mapping(target = "total", ignore = true)})
    Order toEntity(OrderUpdateRequestDTO orderUpdateRequestDTO);

    OrderResponseDTO toResponseDTO(Order order);

}