package com.wishkart.service;

import com.wishkart.dto.ProductDTO;
import com.wishkart.dto.WishlistDTO;
import com.wishkart.entity.Product;
import com.wishkart.entity.User;
import com.wishkart.entity.Wishlist;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.ProductRepository;
import com.wishkart.repository.UserRepository;
import com.wishkart.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user wishlists.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Gets or creates a wishlist for a user.
     */
    @Transactional(readOnly = true)
    public WishlistDTO getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        return mapToDTO(wishlist);
    }

    /**
     * Adds a product to the user's wishlist.
     */
    public WishlistDTO addToWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (wishlist.containsProductId(productId)) {
            throw new BadRequestException("Product is already in your wishlist");
        }

        wishlist.addProduct(product);
        wishlist = wishlistRepository.save(wishlist);

        log.info("Product {} added to wishlist for user {}", productId, userId);
        return mapToDTO(wishlist);
    }

    /**
     * Removes a product from the user's wishlist.
     */
    public WishlistDTO removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!wishlist.containsProductId(productId)) {
            throw new BadRequestException("Product is not in your wishlist");
        }

        wishlist.removeProduct(product);
        wishlist = wishlistRepository.save(wishlist);

        log.info("Product {} removed from wishlist for user {}", productId, userId);
        return mapToDTO(wishlist);
    }

    /**
     * Toggles a product in the wishlist (add if not present, remove if present).
     */
    public WishlistDTO toggleWishlistItem(Long userId, Long productId) {
        if (isInWishlist(userId, productId)) {
            return removeFromWishlist(userId, productId);
        } else {
            return addToWishlist(userId, productId);
        }
    }

    /**
     * Clears the entire wishlist.
     */
    public void clearWishlist(Long userId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));

        wishlist.getProducts().clear();
        wishlistRepository.save(wishlist);

        log.info("Wishlist cleared for user {}", userId);
    }

    /**
     * Checks if a product is in the user's wishlist.
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.isProductInWishlist(userId, productId);
    }

    /**
     * Gets the count of items in the wishlist.
     */
    @Transactional(readOnly = true)
    public int getWishlistCount(Long userId) {
        return wishlistRepository.countItemsByUserId(userId);
    }

    /**
     * Moves an item from wishlist to cart.
     */
    public WishlistDTO moveToCart(Long userId, Long productId, CartService cartService) {
        // Add to cart first
        cartService.addToCart(userId, productId, 1);

        // Then remove from wishlist
        return removeFromWishlist(userId, productId);
    }

    /**
     * Moves all wishlist items to cart.
     */
    public void moveAllToCart(Long userId, CartService cartService) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found"));

        for (Product product : new java.util.ArrayList<>(wishlist.getProducts())) {
            if (product.isActive() && product.getStockQuantity() > 0) {
                try {
                    cartService.addToCart(userId, product.getId(), 1);
                    wishlist.removeProduct(product);
                } catch (Exception e) {
                    log.warn("Could not move product {} to cart: {}", product.getId(), e.getMessage());
                }
            }
        }

        wishlistRepository.save(wishlist);
        log.info("Moved available wishlist items to cart for user {}", userId);
    }

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Wishlist newWishlist = Wishlist.builder()
                    .user(user)
                    .build();
                return wishlistRepository.save(newWishlist);
            });
    }

    private WishlistDTO mapToDTO(Wishlist wishlist) {
        List<ProductDTO> productDTOs = wishlist.getProducts().stream()
            .map(this::mapProductToDTO)
            .collect(Collectors.toList());

        return WishlistDTO.builder()
            .id(wishlist.getId())
            .userId(wishlist.getUser().getId())
            .products(productDTOs)
            .itemCount(wishlist.getItemCount())
            .build();
    }

    private ProductDTO mapProductToDTO(Product product) {
        String primaryImageUrl = product.getImages().stream()
            .filter(img -> img.isPrimary())
            .findFirst()
            .map(img -> img.getImageUrl())
            .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

        return ProductDTO.builder()
            .id(product.getId())
            .name(product.getName())
            .slug(product.getSlug())
            .sku(product.getSku())
            .shortDescription(product.getShortDescription())
            .price(product.getPrice())
            .compareAtPrice(product.getCompareAtPrice())
            .stockQuantity(product.getStockQuantity())
            .active(product.isActive())
            .primaryImageUrl(primaryImageUrl)
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .averageRating(product.getAverageRating())
            .reviewCount(product.getReviewCount())
            .build();
    }
}
