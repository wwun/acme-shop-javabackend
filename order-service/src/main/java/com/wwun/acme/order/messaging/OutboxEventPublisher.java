package com.wwun.acme.order.messaging;

import java.util.UUID;

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

    public void publish(UUID aggregateId, OutboxEventType type, Object eventPayload){

        try{
            String orderAsJsonString = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .type(type.name())
                .payload(orderAsJsonString)
                .build();
            outboxEventRepository.save(outboxEvent);
        }catch(JsonProcessingException ex){
            throw new RuntimeException("Error serializing order evvent payload", ex);
        }
    }

}
