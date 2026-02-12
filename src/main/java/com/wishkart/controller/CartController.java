package com.wishkart.controller;

import com.wishkart.dto.AddToCartRequest;
import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.CartDTO;
import com.wishkart.service.CartService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for shopping cart endpoints.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartDTO>> getCart() {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        CartDTO cart = cartService.addToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/update/{productId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItem(
            @PathVariable Long productId,
            @RequestParam int quantity) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        CartDTO cart = cartService.updateCartItemQuantity(userId, productId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cart));
    }

    @DeleteMapping("/remove/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartDTO>> removeFromCart(@PathVariable Long productId) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        CartDTO cart = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<CartDTO>> clearCart() {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        CartDTO cart = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cart));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount() {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
