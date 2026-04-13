package com.wwun.acme.inventory.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wwun.acme.inventory.entity.OutboxEvent;
import com.wwun.acme.inventory.enums.OutboxEventType;
import com.wwun.acme.inventory.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private final static Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    public void publish(UUID aggregateId, OutboxEventType outboxEventType, Object payload){

        try{

            log.info("Publishing inventory event");

            String payloadAsString = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .type(outboxEventType.name())
                .payload(payloadAsString)
                .build();
            
            outboxEventRepository.save(outboxEvent);

        }catch(JsonProcessingException ex){
            log.error("Error serializing inventory event serializer: ", ex.getMessage());
            throw new RuntimeException("Error serializing inventory event serializer " +ex.getMessage());
        }
        
    
    }

}
