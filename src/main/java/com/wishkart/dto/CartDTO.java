package com.wishkart.dto;

import com.wishkart.entity.Cart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {

    private Long id;
    private List<CartItemDTO> items;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal total;
    private int totalItems;
    private int uniqueItemCount;

    public static CartDTO fromEntity(Cart cart) {
        return CartDTO.builder()
            .id(cart.getId())
            .items(cart.getItems().stream()
                .map(CartItemDTO::fromEntity)
                .collect(Collectors.toList()))
            .couponCode(cart.getCouponCode())
            .discountAmount(cart.getDiscountAmount())
            .subtotal(cart.getSubtotal())
            .total(cart.getTotal())
            .totalItems(cart.getTotalItems())
            .uniqueItemCount(cart.getUniqueItemCount())
            .build();
    }
}
