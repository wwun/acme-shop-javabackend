package com.wwun.acme.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wwun.acme.order.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>{

    List<OutboxEvent> findStatusOrderByCreatedAtAsc(String status);
}
