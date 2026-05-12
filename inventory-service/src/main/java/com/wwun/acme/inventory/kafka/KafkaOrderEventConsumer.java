package com.wwun.acme.inventory.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.inventory.entity.ProcessedEvent;
import com.wwun.acme.inventory.enums.OutboxEventType;
import com.wwun.acme.inventory.event.OrderCreatedEvent;
import com.wwun.acme.inventory.event.StockFailedEvent;
import com.wwun.acme.inventory.event.StockReservedEvent;
import com.wwun.acme.inventory.exception.InsufficientStockException;
import com.wwun.acme.inventory.exception.InventoryNotFoundException;
import com.wwun.acme.inventory.messaging.OutboxEventPublisher;
import com.wwun.acme.inventory.repository.ProcessedEventRepository;
import com.wwun.acme.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaOrderEventConsumer {

    private final InventoryService inventoryService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventConsumer.class);

    private static final String CONSUMER_NAME = "inventory-service";

    @KafkaListener(
        topics = "${kafka.topics.order-created}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload){
        try{
            //se asume qe solo va a tener ordercreated
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            log.info("Received ORDER_CREATED for orderId: {}", event.orderId());

            //idempotncy
            if(processedEventRepository.existsByEventIdAndConsumer(event.orderId(), CONSUMER_NAME)){
                log.warn("Event already processed for orderId: {}", event.orderId());
                return;
            }

            try{
                inventoryService.reserveStock(event);

                StockReservedEvent reservedEvent = new StockReservedEvent(event.orderId(), event.userId());

                outboxEventPublisher.publish(event.orderId(), OutboxEventType.STOCK_RESERVED, reservedEvent);

                log.info("Stock reserved for orderId: {}", event.orderId());

            }catch(InsufficientStockException | InventoryNotFoundException ex){
                log.warn("Stock reservation failed for orderId: {} - {}", event.orderId(), ex.getMessage());

                StockFailedEvent stockFailedEvent = new StockFailedEvent(event.orderId(), event.userId(), ex.getMessage());

                outboxEventPublisher.publish(event.orderId(), OutboxEventType.STOCK_FAILED, stockFailedEvent);
            }

            processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.orderId())
                .consumer(CONSUMER_NAME)
                .build()
            );

        }catch(Exception ex){
            log.error("Error consuming ORDER_CREATED event: {}", ex.getMessage());
            throw new RuntimeException("Error processing ORDER_CREATED");
        }

    }

    
}
