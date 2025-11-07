package com.wwun.acme.cart.cqrs.command;

import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.wwun.acme.cart.cqrs.command.Commands.AddItemToCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.ClearCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.RemoveItemFromCartCommand;
import com.wwun.acme.cart.cqrs.command.Commands.SetItemQuantityCommand;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class CartCommandHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    public CartCommandHandler(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry){
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    public String key(UUID userId){
        return "cart:" + userId;
    }

    public void handle(AddItemToCartCommand command){
        redisTemplate.opsForHash().increment(key(command.userId()), command.productId().toString(), command.quantity());
        meterRegistry.counter("cart_commands_total", "type", "add").increment();
    }

    public void handle(SetItemQuantityCommand command){
        if(command.quantity() <= 0){
            redisTemplate.opsForHash().delete(key(command.userId()), command.productId().toString());
            meterRegistry.counter("cart_commands_total", "type", "removeBySet0").increment();
        }else{
            redisTemplate.opsForHash().put(key(command.userId()), command.productId().toString(), command.quantity());
            meterRegistry.counter("cart_commands_total", "type", "setQty").increment();
        }
    }

    public void handle(RemoveItemFromCartCommand command){
        redisTemplate.opsForHash().delete(key(command.userId()), command.productId().toString());
        meterRegistry.counter("cart_commands_total", "type", "remove").increment();
    }

    public void handle(ClearCartCommand command){
        redisTemplate.delete(key(command.userId()));
        meterRegistry.counter("cart_commands_total", "type", "clear").increment();
    }

}
