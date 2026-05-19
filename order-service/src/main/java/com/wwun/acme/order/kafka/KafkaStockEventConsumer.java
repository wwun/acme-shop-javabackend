package com.wwun.acme.order.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.order.entity.Order;
import com.wwun.acme.order.entity.ProcessedEvent;
import com.wwun.acme.order.enums.OrderStatus;
import com.wwun.acme.order.event.StockFailedEvent;
import com.wwun.acme.order.event.StockReservedEvent;
import com.wwun.acme.order.exception.OrderNotFoundException;
import com.wwun.acme.order.repository.OrderRepository;
import com.wwun.acme.order.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaStockEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaStockEventConsumer.class);
    private static final String STOCK_RESERVED_CONSUMER = "order-service-stock-reserved";
    private static final String STOCK_FAILED_CONSUMER = "order-service-stock-failed";

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.stock-reserved}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeStockReserved(String payload) {
        try {
            StockReservedEvent event = objectMapper.readValue(payload, StockReservedEvent.class);

            if (processedEventRepository.existsByEventIdAndConsumer(event.orderId(), STOCK_RESERVED_CONSUMER)) {
                log.warn("STOCK_RESERVED already processed for orderId: {}", event.orderId());
                return;
            }

            Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

            order.setStatus(OrderStatus.CONFIRMED);

            processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.orderId())
                .consumer(STOCK_RESERVED_CONSUMER)
                .build()
            );

            log.info("Order confirmed after stock reservation for orderId: {}", event.orderId());
        } catch (Exception ex) {
            log.error("Error consuming STOCK_RESERVED event: {}", ex.getMessage());
            throw new RuntimeException("Error processing STOCK_RESERVED", ex);
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.stock-failed}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeStockFailed(String payload) {
        try {
            StockFailedEvent event = objectMapper.readValue(payload, StockFailedEvent.class);

            if (processedEventRepository.existsByEventIdAndConsumer(event.orderId(), STOCK_FAILED_CONSUMER)) {
                log.warn("STOCK_FAILED already processed for orderId: {}", event.orderId());
                return;
            }

            Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

            order.setStatus(OrderStatus.CANCELLED);

            processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.orderId())
                .consumer(STOCK_FAILED_CONSUMER)
                .build()
            );

            log.info("Order cancelled after stock failure for orderId: {} reason: {}", event.orderId(), event.reason());
        } catch (Exception ex) {
            log.error("Error consuming STOCK_FAILED event: {}", ex.getMessage());
            throw new RuntimeException("Error processing STOCK_FAILED", ex);
        }
    }
}
