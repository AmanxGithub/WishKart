package com.wishkart.event;

import com.wishkart.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka payload published on every order status transition
 * (created / confirmed / shipped / delivered / cancelled).
 *
 * Two consumers read this event, so the field list is the union of what each needs
 * — nothing more:
 *   1. Email service  -> renders order-confirmation / shipped / delivered / cancelled templates
 *   2. Analytics/BI    -> revenue, AOV, payment-method mix, coupon effectiveness,
 *                         cancellation reasons, regional sales dashboards
 *
 * Deliberately NOT included: full Order/User entities, customer phone number,
 * internal audit fields (createdBy, version, etc.) — none of that is used by either
 * consumer, so it doesn't belong on the wire.
 *
 * Note: shipping city/state/country are included for regional sales dashboards; the
 * street address line is included only because the email templates render it. In a
 * stricter production setup you'd typically split PII-bearing fields (street address)
 * out of the analytics-facing event entirely rather than share one payload — kept
 * together here for simplicity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    /** PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, REFUNDED — drives which email template fires. */
    private String eventType;

    private String orderNumber;
    private Long orderId;
    private LocalDateTime occurredAt;

    // --- Customer (email delivery + customer-level analytics) ---
    private Long customerId;
    private String customerEmail;
    private String customerFirstName;

    // --- Line items (email itemization + per-product revenue analytics) ---
    private List<OrderEventItem> items;
    private int itemCount;

    // --- Financials (email totals + revenue / AOV / discount-effectiveness analytics) ---
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCode;

    // --- Payment (payment-method mix + payment-funnel analytics) ---
    private String paymentMethod;
    private String paymentStatus;

    // --- Shipping (email address block + regional sales analytics) ---
    private String shippingName;
    private String shippingAddressLine1;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;

    // --- Status-specific extras ---
    private String trackingNumber;        // populated on SHIPPED (email + logistics analytics)
    private String cancellationReason;    // populated on CANCELLED (churn/cancellation analytics)

    public static OrderEvent from(Order order, String eventType) {
        return OrderEvent.builder()
            .eventType(eventType)
            .orderNumber(order.getOrderNumber())
            .orderId(order.getId())
            .occurredAt(LocalDateTime.now())
            .customerId(order.getUser().getId())
            .customerEmail(order.getUser().getEmail())
            .customerFirstName(order.getUser().getFirstName())
            .items(order.getItems().stream().map(OrderEventItem::from).collect(Collectors.toList()))
            .itemCount(order.getItems().size())
            .subtotal(order.getSubtotal())
            .shippingCost(order.getShippingCost())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .couponCode(order.getCouponCode())
            .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
            .paymentStatus(order.getPaymentStatus().name())
            .shippingName(order.getShippingName())
            .shippingAddressLine1(order.getShippingAddressLine1())
            .shippingCity(order.getShippingCity())
            .shippingState(order.getShippingState())
            .shippingPostalCode(order.getShippingPostalCode())
            .shippingCountry(order.getShippingCountry())
            .trackingNumber(order.getTrackingNumber())
            .cancellationReason(order.getCancellationReason())
            .build();
    }
}
