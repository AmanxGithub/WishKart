package com.wishkart.dto;

import com.wishkart.entity.Product;
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
public class ProductDTO {

    private Long id;
    private String name;
    private String sku;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer stockQuantity;
    private boolean inStock;
    private boolean lowStock;
    private boolean active;
    private boolean featured;
    private BigDecimal weight;
    private String weightUnit;
    private Long categoryId;
    private String categoryName;
    private String primaryImage;
    private String primaryImageUrl;
    private List<String> images;
    private double averageRating;
    private int reviewCount;
    private BigDecimal discountPercentage;

    public static ProductDTO fromEntity(Product product) {
        return ProductDTO.builder()
            .id(product.getId())
            .name(product.getName())
            .sku(product.getSku())
            .slug(product.getSlug())
            .description(product.getDescription())
            .shortDescription(product.getShortDescription())
            .price(product.getPrice())
            .compareAtPrice(product.getCompareAtPrice())
            .stockQuantity(product.getStockQuantity())
            .inStock(product.isInStock())
            .lowStock(product.isLowStock())
            .active(product.isActive())
            .featured(product.isFeatured())
            .weight(product.getWeight())
            .weightUnit(product.getWeightUnit())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .primaryImage(product.getPrimaryImageUrl())
            .images(product.getImages().stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList()))
            .averageRating(product.getAverageRating())
            .reviewCount(product.getReviewCount())
            .discountPercentage(product.getDiscountPercentage())
            .build();
    }
}
