package com.wwun.acme.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.order.feign.ProductClient;
import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.order.OrderUpdateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.product.ProductResponseDTO;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.entity.OrderItem;
import com.wwun.acme.order.mapper.OrderMapper;
import com.wwun.acme.order.metric.OrderMetrics;
import com.wwun.acme.order.repository.OrderItemRepository;
import com.wwun.acme.order.repository.OrderRepository;
import com.wwun.acme.security.SecurityUtils;

@Service
public class OrderServiceImpl implements OrderService{

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final OrderMetrics orderMetrics;

    public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper, OrderItemRepository orderItemRepository, ProductClient productClient, OrderMetrics orderMetrics){
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
        this.orderMetrics = orderMetrics;
    }

    @Override
    @Transactional
    public Order save(OrderCreateRequestDTO orderCreateRequestDTO) {

        UUID userId = SecurityUtils.getCurrentUserId();

        Order order = orderMapper.toEntity(orderCreateRequestDTO);
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());

        List<UUID> productsId = orderCreateRequestDTO.getItems().stream().map(OrderItemCreateRequestDTO::getProductId).toList();

        List<ProductResponseDTO> products = productClient.getAllById(productsId);
        
        //no puedo acceder directamente desde item al precio por el dto orderItemCreateRequestDTO qe se llama dede orderCreateDTO qe no trae ese dato, entonces lo qe se hace es traer el dato desde el mismo item, el usuario no puede modificar el precio de esta manera
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemCreateRequestDTO itemDto : orderCreateRequestDTO.getItems()) {
            ProductResponseDTO product = products.stream()
                .filter(p -> p.getId().equals(itemDto.getProductId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setQuantity(itemDto.getQuantity());
            item.setPriceAtPurchase(product.getPrice());

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            items.add(item);
        }
        
        order.setItems(items);
        order.setTotal(total);
        
        Order orderSaved = orderRepository.save(order);
        orderMetrics.incrementOrdersCreated();
        return orderSaved;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));

        if(!SecurityUtils.isAdmin(SecurityContextHolder.getContext().getAuthentication()) && !order.getUserId().equals(SecurityUtils.getCurrentUserId())){
            throw new RuntimeException("Not allowed to access this order");
        }
        return Optional.of(order);
    }

    @Override
    public List<Order> findAll() {
        if (SecurityUtils.isAdmin(SecurityContextHolder.getContext().getAuthentication())) {
            return orderRepository.findAll();
        }
        return orderRepository.findAllByUserId(SecurityUtils.getCurrentUserId());
    }

    @Override
    public List<Order> findAllByUserId(UUID userId){
        if (!userId.equals(SecurityUtils.getCurrentUserId()) && !SecurityUtils.isAdmin(SecurityContextHolder.getContext().getAuthentication())) {
            throw new RuntimeException("Not allowed to access other users' orders");
        }
        return orderRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public Optional<Order> update(UUID id, OrderUpdateRequestDTO orderUpdateRequestDTO) {

        OrderItem existing = orderItemRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        //existing.setQuantity(orderUpdateRequestDTO.getQuantity());

        //ProductResponseDTO product = productClient.getProductById(orderUpdateRequestDTO.getProductId());
        //existing.setPriceAtPurchase(product.getPrice());

        return Optional.empty();//Optional.of(orderItemRepository.save(existing));

    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));

        if(!SecurityUtils.isAdmin(SecurityContextHolder.getContext().getAuthentication()) && !order.getUserId().equals(SecurityUtils.getCurrentUserId())){
            throw new RuntimeException("Not allowed to access this order");
        }
        orderRepository.deleteById(id);
    }

}
