package com.wwun.acme.inventory.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.kafka.core.KafkaTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaInventoryProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaInventoryProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.stock-reserved}")
    private String stockReservedTopic;

    @Value("${kafka.topics.stock-failed}")
    private String stockFailedTopic;

    public void publishStockReserved(String payload){
        log.info("publishing STOCK_RESERVED to kafka");
        kafkaTemplate.send(stockReservedTopic, payload);
    }

    public void publishStockFailed(String payload){
        log.info("publishing STOCK_FAILED to kafka");
        kafkaTemplate.send(stockFailedTopic, payload);
    }

}
