package com.wishkart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon entity for discount codes and promotions.
 */
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String description;

    @NotNull(message = "Discount type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "per_user_limit")
    @Builder.Default
    private Integer perUserLimit = 1;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Category restriction (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category applicableCategory;

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active 
            && now.isAfter(startDate) 
            && now.isBefore(endDate)
            && (usageLimit == null || usageCount < usageLimit);
    }

    public boolean isApplicable(BigDecimal orderAmount) {
        if (!isValid()) {
            return false;
        }
        if (minimumOrderAmount != null && orderAmount.compareTo(minimumOrderAmount) < 0) {
            return false;
        }
        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isApplicable(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue.divide(BigDecimal.valueOf(100)));
        } else {
            discount = discountValue;
        }

        // Apply maximum discount cap
        if (maximumDiscount != null && discount.compareTo(maximumDiscount) > 0) {
            discount = maximumDiscount;
        }

        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }
}
