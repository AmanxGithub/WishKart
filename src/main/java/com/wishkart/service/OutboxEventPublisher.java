package com.wishkart.service;

import com.wishkart.entity.OutboxEvent;
import com.wishkart.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox table for events not yet delivered to Kafka and sends them,
 * one at a time, on this scheduler's own thread — never on an HTTP request thread.
 *
 * This is the ONLY place in the app that talks to Kafka for order events. If Kafka
 * is down, rows simply stay PENDING and get retried on the next tick — nothing is
 * lost, and nothing upstream (checkout, status updates) is ever affected.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 10;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    /**
     * Runs every 3s. Deliberately synchronous (.get()) per event: this method already
     * runs on its own scheduler thread, not a web request thread, so blocking here
     * costs nothing and keeps the retry/failure bookkeeping simple to reason about.
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pending) {
            deliver(event);
            outboxEventRepository.save(event);
        }
    }

    private void deliver(OutboxEvent event) {
        try {
            SendResult<String, String> result = kafkaTemplate
                .send(event.getTopic(), event.getAggregateId(), event.getPayload())
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);

            log.info("Outbox event published: {} [{}] -> partition {}, offset {}",
                event.getAggregateId(), event.getEventType(),
                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());

        } catch (Exception ex) {
            event.setAttemptCount(event.getAttemptCount() + 1);
            event.setLastError(ex.getMessage());

            if (event.getAttemptCount() >= MAX_ATTEMPTS) {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.error("Outbox event permanently failed after {} attempts: {} [{}] - {}",
                    MAX_ATTEMPTS, event.getAggregateId(), event.getEventType(), ex.getMessage());
            } else {
                log.warn("Outbox event publish attempt {}/{} failed for {} [{}]: {}",
                    event.getAttemptCount(), MAX_ATTEMPTS,
                    event.getAggregateId(), event.getEventType(), ex.getMessage());
            }
        }
    }
}
