package com.wwun.acme.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.inventory.entity.OutboxEvent;
import com.wwun.acme.inventory.enums.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>{

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

}
