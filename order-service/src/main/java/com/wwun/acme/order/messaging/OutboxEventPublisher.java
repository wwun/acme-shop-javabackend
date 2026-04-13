package com.wwun.acme.order.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.order.entity.OutboxEvent;
import com.wwun.acme.order.enums.OutboxEventType;
import com.wwun.acme.order.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private final static Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class); 

    public void publish(UUID aggregateId, OutboxEventType type, Object eventPayload){

        try{

            log.info("publishing order event");

            String orderAsJsonString = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .type(type.name())
                .payload(orderAsJsonString)
                .build();
            outboxEventRepository.save(outboxEvent);

        }catch(JsonProcessingException ex){
            log.error("Error serializing order event payload");
            throw new RuntimeException("Error serializing order event payload" + ex.getMessage());
        }
    }

}
