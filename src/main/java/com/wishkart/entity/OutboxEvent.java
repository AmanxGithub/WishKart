package com.wishkart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Transactional outbox row.
 *
 * Written in the SAME database transaction as the business change it describes
 * (e.g. order created/status changed) — so either both the business row and this
 * outbox row commit together, or neither does. A separate {@code OutboxEventPublisher}
 * polls this table on a schedule and pushes rows to Kafka, retrying on failure.
 *
 * This is what gives us "must succeed, eventually" without ever blocking or failing
 * the original request on a Kafka outage: the write to this table is just a normal,
 * always-available local DB insert.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent extends BaseEntity {

    /** Logical entity type the event describes, e.g. "ORDER". */
    @NotBlank
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /** Business identifier of that entity, e.g. the order number. Used as the Kafka message key. */
    @NotBlank
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /** PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED... */
    @NotBlank
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String topic;

    /** Pre-serialized JSON payload (e.g. an {@code OrderEvent}) — built once, sent as-is. */
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
