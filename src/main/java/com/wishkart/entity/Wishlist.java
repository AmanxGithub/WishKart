package com.wishkart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Wishlist entity for storing user's saved products.
 */
@Entity
@Table(name = "wishlists", indexes = {
    @Index(name = "idx_wishlist_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "wishlist_items",
        joinColumns = @JoinColumn(name = "wishlist_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        if (!products.contains(product)) {
            products.add(product);
        }
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }

    public boolean containsProduct(Product product) {
        return products.contains(product);
    }

    public boolean containsProductId(Long productId) {
        return products.stream().anyMatch(p -> p.getId().equals(productId));
    }

    public int getItemCount() {
        return products.size();
    }
}
