package com.wwun.acme.order.mapper;

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderResponseDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemUpdateRequestDTO;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.entity.OrderItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-09-09T20:47:37-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.43.0.v20250819-1513, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public Order toEntity(OrderCreateRequestDTO orderCreateRequestDTO) {
        if ( orderCreateRequestDTO == null ) {
            return null;
        }

        Order order = new Order();

        order.setUserId( orderCreateRequestDTO.getUserId() );
        order.setItems( orderItemCreateRequestDTOListToOrderItemList( orderCreateRequestDTO.getItems() ) );

        return order;
    }

    @Override
    public Order toEntity(OrderUpdateRequestDTO orderUpdateRequestDTO) {
        if ( orderUpdateRequestDTO == null ) {
            return null;
        }

        Order order = new Order();

        order.setUserId( orderUpdateRequestDTO.getUserId() );
        order.setItems( orderItemUpdateRequestDTOListToOrderItemList( orderUpdateRequestDTO.getItems() ) );

        return order;
    }

    @Override
    public OrderResponseDTO toResponseDTO(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponseDTO orderResponseDTO = new OrderResponseDTO();

        orderResponseDTO.setId( order.getId() );
        orderResponseDTO.setOrderDate( order.getOrderDate() );
        orderResponseDTO.setTotal( order.getTotal() );
        List<OrderItem> list = order.getItems();
        if ( list != null ) {
            orderResponseDTO.setItems( new ArrayList<OrderItem>( list ) );
        }
        orderResponseDTO.setUserId( order.getUserId() );

        return orderResponseDTO;
    }

    protected List<OrderItem> orderItemCreateRequestDTOListToOrderItemList(List<OrderItemCreateRequestDTO> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItem> list1 = new ArrayList<OrderItem>( list.size() );
        for ( OrderItemCreateRequestDTO orderItemCreateRequestDTO : list ) {
            list1.add( orderItemMapper.toEntity( orderItemCreateRequestDTO ) );
        }

        return list1;
    }

    protected List<OrderItem> orderItemUpdateRequestDTOListToOrderItemList(List<OrderItemUpdateRequestDTO> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItem> list1 = new ArrayList<OrderItem>( list.size() );
        for ( OrderItemUpdateRequestDTO orderItemUpdateRequestDTO : list ) {
            list1.add( orderItemMapper.toEntity( orderItemUpdateRequestDTO ) );
        }

        return list1;
    }
}
