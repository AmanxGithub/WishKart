package com.wishkart.event;

import com.wishkart.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Line item slice of {@link OrderEvent}.
 * Kept minimal: only what order-confirmation/shipment emails render (product name,
 * quantity, price, line total) and what per-product revenue analytics needs (productId).
 * Deliberately excludes description, images, stock, category, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventItem {

    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public static OrderEventItem from(OrderItem item) {
        return OrderEventItem.builder()
            .productId(item.getProduct() != null ? item.getProduct().getId() : null)
            .productName(item.getProductName())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .lineTotal(item.getTotal())
            .build();
    }
}
