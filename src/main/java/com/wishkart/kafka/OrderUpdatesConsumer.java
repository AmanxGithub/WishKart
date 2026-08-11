package com.wishkart.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishkart.entity.Order;
import com.wishkart.event.OrderEvent;
import com.wishkart.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderUpdatesConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @RetryableTopic(
            attempts = "4", // 1 main attempt + 3 retries
            backoff = @Backoff(
                    delay = 60000,      // First retry delay: 1 minute (60,000 ms)
                    multiplier = 5.0,   // Multiplies the previous delay by 5x each step
                    maxDelay = 600000   // Caps the delay at 10 minutes (600,000 ms)
            ),
            autoCreateTopics = "true" // Spring will auto-provision the retry and DLQ topics
            //39-40-41-46-56
    )
    @KafkaListener(topics = "orders", groupId = "consumerGroupOrder", concurrency = "2")
    public void consume(String message) {
         System.out.println("Processing message: " + message);
        try {
            OrderEvent orderEvent= objectMapper.readValue(message, OrderEvent.class);
            emailService.sendOrderStatusUpdateEmail(orderEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Order event JSON parsing failed.", e);
        }

    }
}
