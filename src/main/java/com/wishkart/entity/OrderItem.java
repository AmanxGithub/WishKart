package com.wishkart.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Order Item entity representing an item in an order.
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order", columnList = "order_id"),
    @Index(name = "idx_order_item_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Store product details as snapshot at time of order
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku;

    @Column(name = "product_image")
    private String productImage;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.total = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
    }

    public static OrderItem fromCartItem(CartItem cartItem) {
        Product product = cartItem.getProduct();
        return OrderItem.builder()
            .product(product)
            .productName(product.getName())
            .productSku(product.getSku())
            .productImage(product.getPrimaryImageUrl())
            .quantity(cartItem.getQuantity())
            .unitPrice(cartItem.getPrice())
            .discountAmount(BigDecimal.ZERO)
            .total(cartItem.getTotal())
            .build();
    }
}
