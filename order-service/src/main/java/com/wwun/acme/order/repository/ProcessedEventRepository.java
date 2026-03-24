package com.wwun.acme.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.order.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID>{

    boolean existsByEventIdAndConsumer(UUID eventId, String consumer);
}
