package com.wwun.acme.order.mapper;

import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemResponseDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.entity.OrderItem;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-17T15:37:22-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.43.0.v20250819-1513, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItem toEntity(OrderItemCreateRequestDTO orderCreateRequestDTO) {
        if ( orderCreateRequestDTO == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        orderItem.setProductId( orderCreateRequestDTO.getProductId() );
        orderItem.setQuantity( orderCreateRequestDTO.getQuantity() );

        return orderItem;
    }

    @Override
    public OrderItem toEntity(OrderItemUpdateRequestDTO orderUpdateRequestDTO) {
        if ( orderUpdateRequestDTO == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        orderItem.setId( orderUpdateRequestDTO.getId() );
        orderItem.setProductId( orderUpdateRequestDTO.getProductId() );
        orderItem.setQuantity( orderUpdateRequestDTO.getQuantity() );

        return orderItem;
    }

    @Override
    public OrderItemResponseDTO toResponseDTO(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }

        OrderItemResponseDTO orderItemResponseDTO = new OrderItemResponseDTO();

        orderItemResponseDTO.setId( orderItem.getId() );
        orderItemResponseDTO.setProductId( orderItem.getProductId() );
        orderItemResponseDTO.setQuantity( orderItem.getQuantity() );
        orderItemResponseDTO.setPriceAtPurchase( orderItem.getPriceAtPurchase() );

        return orderItemResponseDTO;
    }
}
