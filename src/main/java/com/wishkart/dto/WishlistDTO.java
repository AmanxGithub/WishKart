package com.wishkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {

    private Long id;
    private Long userId;
    private List<ProductDTO> products;
    private int itemCount;
}
