package com.wishkart.dto;

import com.wishkart.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private String productSku;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal total;
    private boolean inStock;
    private Integer availableStock;

    public static CartItemDTO fromEntity(CartItem item) {
        return CartItemDTO.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .productName(item.getProduct().getName())
            .productSlug(item.getProduct().getSlug())
            .productImage(item.getProduct().getPrimaryImageUrl())
            .productSku(item.getProduct().getSku())
            .price(item.getPrice())
            .quantity(item.getQuantity())
            .total(item.getTotal())
            .inStock(item.getProduct().isInStock())
            .availableStock(item.getProduct().getStockQuantity())
            .build();
    }
}
