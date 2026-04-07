package com.wwun.acme.inventory.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import lombok.Setter;

@Entity
@Table(name = "processed_events", 
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"event_id", "consumer"}
    )
)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID eventId;

    @NotBlank
    @Column(nullable = false)
    private String consumer;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist(){
        processedAt = Instant.now();
    }

}
