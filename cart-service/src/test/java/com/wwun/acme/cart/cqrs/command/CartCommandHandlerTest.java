package com.wwun.acme.cart.cqrs.command;

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

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wwun.acme.cart.cqrs.command.Commands.AddItemToCartCommand;

@ExtendWith(MockitoExtension.class)
public class CartCommandHandlerTest {

    @InjectMocks CartCommandHandler CartCommandHandler;
    
    @Test
    void key_shouldReturnCartPrefixWithUserId(){

        UUID userId = UUID.randomUUID();

        String messageReturn = CartCommandHandler.key(userId);

        assertEquals("cart:"+userId, messageReturn);

    }
    
    void handleAddItemToCart_shouldIncrementProductQuantityInRedis(){

        // AddItemToCartCommand command){
        //     redisTemplate.opsForHash().increment(key(command.userId()), command.productId().toString(), command.quantity());
        //     meterRegistry.counter("cart_commands_total", "type", "add").increment

        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 20;

        AddItemToCartCommand command = new AddItemToCartCommand(userId, productId, quantity);

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        ArgumentCaptor<RedisTemplate> captor = ArgumentCaptor.forClass(RedisTemplate.class);

        verify(redisTemplate).opsForHash().entries(command.userId().toString());

        assertEquals(captor.getValue(), 20);

    }

    // handleAddItemToCart_shouldIncrementAddMetric
    // handleSetItemQuantity_shouldPutQuantityWhenGreaterThanZero
    // handleSetItemQuantity_shouldIncrementSetQtyMetricWhenGreaterThanZero
    // handleSetItemQuantity_shouldDeleteItemWhenQuantityIsZero
    // handleSetItemQuantity_shouldDeleteItemWhenQuantityIsNegative
    // handleSetItemQuantity_shouldIncrementRemoveBySet0MetricWhenQuantityIsZeroOrLess
    // handleRemoveItemFromCart_shouldDeleteItemFromRedis
    // handleRemoveItemFromCart_shouldIncrementRemoveMetric
    // handleClearCart_shouldDeleteCartKey
    // handleClearCart_shouldIncrementClearMetric

}
