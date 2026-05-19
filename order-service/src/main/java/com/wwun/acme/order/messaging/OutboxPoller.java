package com.wwun.acme.order.messaging;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.order.entity.OutboxEvent;
import com.wwun.acme.order.enums.OutboxEventStatus;
import com.wwun.acme.order.enums.OutboxEventType;
import com.wwun.acme.order.kafka.KafkaOrderProducer;
import com.wwun.acme.order.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaOrderProducer kafkaOrderProducer;

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void poll(){

        List<OutboxEvent> pendingList = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for(OutboxEvent event : pendingList){
            try{
                if(event.getType().equals(OutboxEventType.ORDER_CREATED.name())){
                    kafkaOrderProducer.publishOrderCreated(event.getPayload());
                }
                event.markAsProcessed();
            }catch(Exception ex){
                log.error("Error processing outbox event {}: {}", event.getId(), ex.getMessage());
                event.markAsFailed();
            }
        }
    }

}
