package com.wwun.acme.inventory.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.inventory.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID>{

    boolean existsByEventIdAndConsumer(UUID eventId, String consumer);

}
