package com.wwun.acme.inventory.messaging;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wwun.acme.inventory.entity.OutboxEvent;
import com.wwun.acme.inventory.enums.OutboxEventStatus;
import com.wwun.acme.inventory.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void poll(){

        List<OutboxEvent> pendingList = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        for(OutboxEvent event : pendingList){
            try{
                System.out.println("process the event to be publish and change outboxevent status");
                event.markAsProcessed();
            }catch(Exception ex){
                System.out.println("error processing decide which kind of exception to throw");
                event.markAsFailed();
            }
        }

    }

}
