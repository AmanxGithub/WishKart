package com.wishkart.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity representing items for sale.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku", columnList = "sku", unique = true),
    @Index(name = "idx_product_slug", columnList = "slug", unique = true),
    @Index(name = "idx_product_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank(message = "SKU is required")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank(message = "Slug is required")
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "track_inventory")
    @Builder.Default
    private boolean trackInventory = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "weight_unit", length = 10)
    @Builder.Default
    private String weightUnit = "kg";

    // Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;

    // Product images
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @JsonManagedReference
    private List<ProductImage> images = new ArrayList<>();

    // Product reviews
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    // Order items containing this product
    @OneToMany(mappedBy = "product")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // SEO fields
    @Column(name = "meta_title", length = 100)
    private String metaTitle;

    @Column(name = "meta_description", length = 300)
    private String metaDescription;

    // Rating cache (updated by ReviewService)
    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }

    @JsonIgnore
    public String getPrimaryImageUrl() {
        return images.stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .map(ProductImage::getImageUrl)
            .orElse(images.isEmpty() ? "/images/placeholder.png" : images.get(0).getImageUrl());
    }

    @JsonIgnore
    public boolean isInStock() {
        return !trackInventory || stockQuantity > 0;
    }

    @JsonIgnore
    public boolean isLowStock() {
        return trackInventory && stockQuantity <= lowStockThreshold;
    }

    @JsonIgnore
    public BigDecimal getDiscountPercentage() {
        if (compareAtPrice == null || compareAtPrice.compareTo(price) <= 0) {
            return BigDecimal.ZERO;
        }
        return compareAtPrice.subtract(price)
            .divide(compareAtPrice, 2, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    public Double getAverageRating() {
        return this.averageRating != null ? this.averageRating : 0.0;
    }

    public void setAverageRating(Double rating) {
        this.averageRating = rating != null ? rating : 0.0;
    }

    public Integer getReviewCount() {
        return this.reviewCount != null ? this.reviewCount : 0;
    }

    public void setReviewCount(Integer count) {
        this.reviewCount = count != null ? count : 0;
    }

    public void decreaseStock(int quantity) {
        if (trackInventory) {
            if (stockQuantity < quantity) {
                throw new IllegalStateException("Insufficient stock for product: " + name);
            }
            this.stockQuantity -= quantity;
        }
    }

    public void increaseStock(int quantity) {
        if (trackInventory) {
            this.stockQuantity += quantity;
        }
    }
}
