package com.wishkart.dto;

import com.wishkart.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<OrderItemDTO> items;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentIntentId;
    private LocalDateTime paidAt;

    // Shipping
    private String shippingName;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhone;

    // Tracking
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    private String customerNotes;
    private int totalItemCount;
    private LocalDateTime createdAt;

    public static OrderDTO fromEntity(Order order) {
        return OrderDTO.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUser().getId())
            .userName(order.getUser().getFullName())
            .userEmail(order.getUser().getEmail())
            .items(order.getItems().stream()
                .map(OrderItemDTO::fromEntity)
                .collect(Collectors.toList()))
            .status(order.getStatus().name())
            .subtotal(order.getSubtotal())
            .shippingCost(order.getShippingCost())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .couponCode(order.getCouponCode())
            .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
            .paymentStatus(order.getPaymentStatus().name())
            .paymentIntentId(order.getPaymentIntentId())
            .paidAt(order.getPaidAt())
            .shippingName(order.getShippingName())
            .shippingAddress(order.getShippingAddressLine1())
            .shippingCity(order.getShippingCity())
            .shippingState(order.getShippingState())
            .shippingPostalCode(order.getShippingPostalCode())
            .shippingCountry(order.getShippingCountry())
            .shippingPhone(order.getShippingPhone())
            .trackingNumber(order.getTrackingNumber())
            .shippedAt(order.getShippedAt())
            .deliveredAt(order.getDeliveredAt())
            .cancelledAt(order.getCancelledAt())
            .cancellationReason(order.getCancellationReason())
            .customerNotes(order.getCustomerNotes())
            .totalItemCount(order.getTotalItemCount())
            .createdAt(order.getCreatedAt())
            .build();
    }
}
