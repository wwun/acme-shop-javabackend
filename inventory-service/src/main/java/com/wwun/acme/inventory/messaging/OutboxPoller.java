package com.wwun.acme.inventory.messaging;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.inventory.entity.OutboxEvent;
import com.wwun.acme.inventory.enums.OutboxEventStatus;
import com.wwun.acme.inventory.enums.OutboxEventType;
import com.wwun.acme.inventory.kafka.KafkaInventoryProducer;
import com.wwun.acme.inventory.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaInventoryProducer kafkaInventoryProducer;

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void poll(){

        List<OutboxEvent> pendingList = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for(OutboxEvent event : pendingList){
            try{
                if(OutboxEventType.STOCK_RESERVED.name().equals(event.getType())){
                    kafkaInventoryProducer.publishStockReserved(event.getPayload());
                } else if(OutboxEventType.STOCK_FAILED.name().equals(event.getType())){
                    kafkaInventoryProducer.publishStockFailed(event.getPayload());
                } else {
                    throw new IllegalArgumentException("Unsupported outbox event type: " + event.getType());
                }
                event.markAsProcessed();
            }catch(Exception ex){
                log.error("error processing outbox event {}: {}", event.getId(), ex.getMessage());                
                event.markAsFailed();
            }
        }

    }

}
