package com.wishkart.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.wishkart.dto.OrderDTO;
import com.wishkart.entity.Order;
import com.wishkart.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for payment processing with Stripe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.currency}")
    private String currency;

    private final OrderService orderService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Creates a payment intent for the given order.
     */
    public Map<String, Object> createPaymentIntent(Long orderId) {
        OrderDTO order = orderService.getOrderById(orderId);

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Payment has already been processed for this order");
        }

        try {
            // Convert amount to cents
            long amountInCents = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setDescription("WishKart Order: " + order.getOrderNumber())
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("order_number", order.getOrderNumber())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Update order with payment intent ID
            orderService.updatePaymentStatus(orderId, Order.PaymentStatus.PROCESSING, paymentIntent.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());

            log.info("Payment intent created for order {}: {}", order.getOrderNumber(), paymentIntent.getId());
            return response;

        } catch (StripeException e) {
            log.error("Stripe error creating payment intent: {}", e.getMessage());
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }

    /**
     * Confirms a payment after successful processing.
     */
    public OrderDTO confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            String orderId = paymentIntent.getMetadata().get("order_id");
            if (orderId == null) {
                throw new BadRequestException("Invalid payment intent - no order ID found");
            }

            Order.PaymentStatus status = switch (paymentIntent.getStatus()) {
                case "succeeded" -> Order.PaymentStatus.COMPLETED;
                case "processing" -> Order.PaymentStatus.PROCESSING;
                case "requires_payment_method", "requires_confirmation", "requires_action" -> Order.PaymentStatus.PENDING;
                default -> Order.PaymentStatus.FAILED;
            };

            OrderDTO order = orderService.updatePaymentStatus(
                Long.parseLong(orderId),
                status,
                paymentIntentId
            );

            log.info("Payment {} for order {}", status, order.getOrderNumber());
            return order;

        } catch (StripeException e) {
            log.error("Stripe error confirming payment: {}", e.getMessage());
            throw new BadRequestException("Failed to confirm payment: " + e.getMessage());
        }
    }

    /**
     * Processes a refund for an order.
     */
    public OrderDTO processRefund(Long orderId, BigDecimal amount, String reason) {
        OrderDTO order = orderService.getOrderById(orderId);

        if (!"COMPLETED".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Cannot refund an order that hasn't been paid");
        }

        if (order.getPaymentIntentId() == null) {
            throw new BadRequestException("No payment intent found for this order");
        }

        try {
            // Create refund
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", order.getPaymentIntentId());

            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                params.put("amount", amountInCents);
            }

            if (reason != null) {
                params.put("reason", "requested_by_customer");
            }

            com.stripe.model.Refund.create(params);

            // Update order status
            return orderService.updatePaymentStatus(orderId, Order.PaymentStatus.REFUNDED, null);

        } catch (StripeException e) {
            log.error("Stripe error processing refund: {}", e.getMessage());
            throw new BadRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Handles Stripe webhook events.
     */
    public void handleWebhookEvent(String payload, String signature) {
        // Webhook handling implementation
        // In production, verify the webhook signature and process events
        log.info("Webhook received: {}", payload);
    }
}
