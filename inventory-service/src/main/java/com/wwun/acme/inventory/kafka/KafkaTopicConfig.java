package com.wwun.acme.inventory.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.stock-reserved}")
    private String stockReservedTopic;

    @Value("${kafka.topics.stock-failed}")
    private String stockFailedTopic;

    @Bean
    public NewTopic orderCreatedTopic(){
        return TopicBuilder.name(orderCreatedTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockReservedTopic() {
        return TopicBuilder.name(stockReservedTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stockFailedTopic() {
        return TopicBuilder.name(stockFailedTopic).partitions(1).replicas(1).build();
    }

}
