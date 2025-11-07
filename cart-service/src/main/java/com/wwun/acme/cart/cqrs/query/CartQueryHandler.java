package com.wwun.acme.cart.cqrs.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.wwun.acme.cart.cqrs.query.Queries.GetCartQuery;
import com.wwun.acme.cart.cqrs.query.Queries.GetCartSummaryQuery;
import com.wwun.acme.cart.dto.product.ProductResponseDTO;
import com.wwun.acme.cart.dto.response.CartItemResponseDTO;
import com.wwun.acme.cart.dto.response.CartResponseDTO;
import com.wwun.acme.cart.dto.response.CartSummaryDTO;
import com.wwun.acme.cart.dto.response.CartSummaryItemDTO;
import com.wwun.acme.cart.feign.ProductClient;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class CartQueryHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductClient productClient;
    private final MeterRegistry meterRegistry;

    public CartQueryHandler(RedisTemplate<String, Object> redisTemplate, ProductClient productClient, MeterRegistry meterRegistry){
        this.redisTemplate = redisTemplate;
        this.productClient = productClient;
        this.meterRegistry = meterRegistry;
    }

    private String key(UUID userId){
        return "cart:" + userId;
    }

    public CartResponseDTO handle(GetCartQuery query){
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(query.userId()));
        List<CartItemResponseDTO> items = entries.entrySet().stream().map(entry -> new CartItemResponseDTO(UUID.fromString((String)entry.getKey()),
                                ((Number)entry.getValue()).intValue())).toList();
        meterRegistry.counter("cart_queries_total", "type", "getCart").increment();
        return new CartResponseDTO(query.userId(), items);
    }

    public CartSummaryDTO handle(GetCartSummaryQuery query){
        long start = System.nanoTime();

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(query.userId()));
        List<CartSummaryItemDTO> items = new ArrayList<>();
        int totalItems = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (var entry : entries.entrySet()) {
            UUID productId = UUID.fromString((String)entry.getKey());
            int quantity = ((Number)entry.getValue()).intValue();
            ProductResponseDTO product = productClient.getById(productId);
            items.add(new CartSummaryItemDTO(productId, product.name(), product.price(), quantity));
            totalItems += quantity;
            total = total.add(product.price().multiply(BigDecimal.valueOf(quantity)));
        }

        meterRegistry.timer("cart_query_latency","type","summary").record(System.nanoTime()-start, TimeUnit.NANOSECONDS);

        return new CartSummaryDTO(query.userId(), items, totalItems, total);
    }

}
