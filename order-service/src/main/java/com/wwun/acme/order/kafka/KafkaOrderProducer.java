package com.wwun.acme.order.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(KafkaOrderProducer.class);

    @Value("{kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(String payload){
        log.info("publishing order created to kafka topic: {}", orderCreatedTopic);
        kafkaTemplate.send(orderCreatedTopic, payload);
    }

}
