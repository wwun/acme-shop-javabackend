package com.wwun.acme.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wwun.acme.order.dto.order.order.OrderCreateRequestDTO;
import com.wwun.acme.order.dto.order.orderItem.OrderItemCreateRequestDTO;
import com.wwun.acme.order.dto.product.ProductResponseDTO;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.entity.OrderItem;
import com.wwun.acme.order.mapper.OrderMapper;
import com.wwun.acme.order.metric.OrderMetrics;
import com.wwun.acme.order.repository.OrderRepository;
import com.wwun.acme.security.AuthUserPrincipal;
import com.wwun.acme.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock ProductGatewayService productGatewayService;
    @Mock OrderMetrics orderMetrics;

    @InjectMocks OrderServiceImpl orderServiceImpl;

    @AfterEach
    void cleanUp(){
        SecurityContextHolder.clearContext();
    }

    @Test
    void save_shouldCalculateTotalAndPersistOrder(){
        
        //Given
        UUID userId = UUID.randomUUID();

        AuthUserPrincipal principal = new AuthUserPrincipal(userId, "wwun");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        Order orderMapped = new Order();

        OrderCreateRequestDTO orderCreateRequestDTO = new OrderCreateRequestDTO();
        when(orderMapper.toEntity(orderCreateRequestDTO)).thenReturn(orderMapped);

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        OrderItemCreateRequestDTO orderItemCreateRequestDTO1 = new OrderItemCreateRequestDTO();
        orderItemCreateRequestDTO1.setProductId(productId1);
        orderItemCreateRequestDTO1.setQuantity(2);
        
        OrderItemCreateRequestDTO orderItemCreateRequestDTO2 = new OrderItemCreateRequestDTO();
        orderItemCreateRequestDTO2.setProductId(productId2);
        orderItemCreateRequestDTO2.setQuantity(1);

        orderCreateRequestDTO.setItems(List.of(orderItemCreateRequestDTO1, orderItemCreateRequestDTO2));
        
        ProductResponseDTO productResponseDTO1 = new ProductResponseDTO();
        productResponseDTO1.setId(productId1);
        productResponseDTO1.setPrice(new BigDecimal("10.00"));

        ProductResponseDTO productResponseDTO2 = new ProductResponseDTO();
        productResponseDTO2.setId(productId2);
        productResponseDTO2.setPrice(new BigDecimal("5.50"));

        List<UUID> productsId = List.of(productId1, productId2);

        when(productGatewayService.getAllById(productsId)).thenReturn(List.of(productResponseDTO1, productResponseDTO2));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(orders -> orders.getArgument(0));

        //When
        Order saved = orderServiceImpl.save(orderCreateRequestDTO);

        //Then
        assertNotNull(saved);
        assertEquals(userId, saved.getUserId());
        assertNotNull(saved.getOrderDate());
        assertNotNull(saved.getItems());
        assertEquals(2, saved.getItems().size());
        assertEquals(new BigDecimal("25.50"), saved.getTotal());
        
        assertTrue(saved.getItems().stream().allMatch(item -> item.getPriceAtPurchase() != null));
        assertTrue(saved.getItems().stream().allMatch(item -> item.getOrder() == saved));

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderMetrics, times(1)).incrementOrdersCreated();

    }

    @Test
    void save_shouldThrow_whenProductNotFoundAndNotSave(){
        
        //Given
        UUID userId = UUID.randomUUID();

        AuthUserPrincipal principal = new AuthUserPrincipal(userId, "wwun");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        UUID productId = UUID.randomUUID();
        UUID productNotFoundId = UUID.randomUUID();

        OrderItemCreateRequestDTO item1 = new OrderItemCreateRequestDTO();
        item1.setProductId(productId);
        item1.setQuantity(1);

        OrderItemCreateRequestDTO item2 = new OrderItemCreateRequestDTO();
        item2.setProductId(productNotFoundId);
        item2.setQuantity(2);

        OrderCreateRequestDTO orderCreateRequestDTO = new OrderCreateRequestDTO();
        orderCreateRequestDTO.setItems(List.of(item1, item2));

        Order orderMapped = new Order();
        when(orderMapper.toEntity(orderCreateRequestDTO)).thenReturn(orderMapped);

        List<UUID> productsId = List.of(productId, productNotFoundId);

        ProductResponseDTO productResponseDTO = new ProductResponseDTO();
        productResponseDTO.setId(productId);
        productResponseDTO.setPrice(new BigDecimal("10.00"));

        when(productGatewayService.getAllById(productsId))
                .thenReturn(List.of(productResponseDTO));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderServiceImpl.save(orderCreateRequestDTO));

        // Then
        assertTrue(ex.getMessage().toLowerCase().contains("producto no encontrado"));

        verify(orderRepository, never()).save(any());
        verify(orderMetrics, never()).incrementOrdersCreated();
    }

    @Test
    void findAll_shouldReturnListOfOrdersByUser(){
        UUID userId = UUID.randomUUID();

        AuthUserPrincipal authUserPrincipal = new AuthUserPrincipal(userId, "wwun");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(authUserPrincipal);

        //isAdmin:SecurityUtils
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();

        //SecurityContextHolder
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        List<Order> orderList = List.of(new Order(), new Order());
        when(orderRepository.findAllByUserId(userId)).thenReturn(orderList);

        List<Order> result = orderServiceImpl.findAll();

        assertSame(orderList, result);

        verify(orderRepository, times(1)).findAllByUserId(userId);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void findAll_shouldReturnAllOrders_whenAdmin(){
        Authentication auth = mock(Authentication.class);

        //isAdmin:SecurityUtils
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

        //SecurityContextHolder
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        List<Order> expectedOrderList = List.of(new Order(),new Order());

        when(orderRepository.findAll()).thenReturn(expectedOrderList);

        List<Order> result = orderRepository.findAll();

        assertSame(expectedOrderList, result);
        
        verify(orderRepository).findAll();
        verify(orderRepository, never()).findAllByUserId(any());

    }
}
