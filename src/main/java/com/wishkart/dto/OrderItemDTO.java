package com.wishkart.dto;

import com.wishkart.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal total;

    public static OrderItemDTO fromEntity(OrderItem item) {
        return OrderItemDTO.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .productName(item.getProductName())
            .productSku(item.getProductSku())
            .productImage(item.getProductImage())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .discountAmount(item.getDiscountAmount())
            .total(item.getTotal())
            .build();
    }
}
