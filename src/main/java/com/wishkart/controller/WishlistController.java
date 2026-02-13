package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.WishlistDTO;
import com.wishkart.service.CartService;
import com.wishkart.service.WishlistService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for wishlist operations.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist operations")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO>> getWishlist() {
        Long userId = getCurrentUserId();
        WishlistDTO wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping("/add/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO>> addToWishlist(@PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId();
        WishlistDTO wishlist = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlist));
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<WishlistDTO>> removeFromWishlist(@PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId();
        WishlistDTO wishlist = wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", wishlist));
    }

    @PostMapping("/toggle/{productId}")
    @Operation(summary = "Toggle product in wishlist (add/remove)")
    public ResponseEntity<ApiResponse<WishlistDTO>> toggleWishlistItem(@PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId();
        WishlistDTO wishlist = wishlistService.toggleWishlistItem(userId, productId);
        String message = wishlist.getProducts().stream()
            .anyMatch(p -> p.getId().equals(productId))
            ? "Added to wishlist"
            : "Removed from wishlist";
        return ResponseEntity.ok(ApiResponse.success(message, wishlist));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear entire wishlist")
    public ResponseEntity<ApiResponse<Void>> clearWishlist() {
        Long userId = getCurrentUserId();
        wishlistService.clearWishlist(userId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared"));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(@PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId();
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }

    @GetMapping("/count")
    @Operation(summary = "Get wishlist item count")
    public ResponseEntity<ApiResponse<Integer>> getWishlistCount() {
        Long userId = getCurrentUserId();
        int count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/move-to-cart/{productId}")
    @Operation(summary = "Move item from wishlist to cart")
    public ResponseEntity<ApiResponse<WishlistDTO>> moveToCart(@PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId();
        WishlistDTO wishlist = wishlistService.moveToCart(userId, productId, cartService);
        return ResponseEntity.ok(ApiResponse.success("Moved to cart", wishlist));
    }

    @PostMapping("/move-all-to-cart")
    @Operation(summary = "Move all wishlist items to cart")
    public ResponseEntity<ApiResponse<Void>> moveAllToCart() {
        Long userId = getCurrentUserId();
        wishlistService.moveAllToCart(userId, cartService);
        return ResponseEntity.ok(ApiResponse.success("Available items moved to cart"));
    }

    private Long getCurrentUserId() {
        return SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }
}
