package com.wishkart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity representing a customer's order.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Pricing
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Coupon
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    // Payment
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_intent_id", length = 100)
    private String paymentIntentId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Shipping Address (stored as snapshot)
    @Column(name = "shipping_name", length = 100)
    private String shippingName;

    @Column(name = "shipping_address_line1")
    private String shippingAddressLine1;

    @Column(name = "shipping_address_line2")
    private String shippingAddressLine2;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_state", length = 100)
    private String shippingState;

    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Column(name = "shipping_country", length = 100)
    private String shippingCountry;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    // Billing Address
    @Column(name = "billing_same_as_shipping")
    @Builder.Default
    private boolean billingSameAsShipping = true;

    @Column(name = "billing_address_line1")
    private String billingAddressLine1;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 100)
    private String billingState;

    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", length = 100)
    private String billingCountry;

    // Tracking
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Notes
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @PrePersist
    public void generateOrderNumber() {
        if (this.orderNumber == null) {
            this.orderNumber = "WK-" + System.currentTimeMillis() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    public int getTotalItemCount() {
        return items.stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
            .map(OrderItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = subtotal
            .add(shippingCost != null ? shippingCost : BigDecimal.ZERO)
            .add(taxAmount != null ? taxAmount : BigDecimal.ZERO)
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }

    public void setShippingAddress(Address address, String name) {
        this.shippingName = name;
        this.shippingAddressLine1 = address.getAddressLine1();
        this.shippingAddressLine2 = address.getAddressLine2();
        this.shippingCity = address.getCity();
        this.shippingState = address.getState();
        this.shippingPostalCode = address.getPostalCode();
        this.shippingCountry = address.getCountry();
    }

    public String getFullShippingAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(shippingAddressLine1);
        if (shippingAddressLine2 != null && !shippingAddressLine2.isEmpty()) {
            sb.append(", ").append(shippingAddressLine2);
        }
        sb.append(", ").append(shippingCity);
        sb.append(", ").append(shippingState);
        sb.append(" ").append(shippingPostalCode);
        sb.append(", ").append(shippingCountry);
        return sb.toString();
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        STRIPE,
        COD
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED,
        CANCELLED
    }
}
