package com.wwun.acme.order.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events", 
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"event_id", "consumer"}
    )
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProcessedEvent {
    
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false)
    @NotNull
    private UUID eventId;

    @Column(nullable = false)
    @NotBlank
    private String consumer;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist(){
        this.processedAt = Instant.now();
    }
}
