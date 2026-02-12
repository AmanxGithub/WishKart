package com.wishkart.service;

import com.wishkart.dto.CartDTO;
import com.wishkart.entity.Cart;
import com.wishkart.entity.CartItem;
import com.wishkart.entity.Coupon;
import com.wishkart.entity.Product;
import com.wishkart.entity.User;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.InsufficientStockException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CartItemRepository;
import com.wishkart.repository.CartRepository;
import com.wishkart.repository.CouponRepository;
import com.wishkart.repository.ProductRepository;
import com.wishkart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for shopping cart operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional(readOnly = true)
    public CartDTO getCartByUserId(Long userId) {
        return getCart(userId);
    }

    @Transactional
    public CartDTO addToCart(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!product.isActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (product.isTrackInventory() && product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStockQuantity());
        }

        // Check if item already in cart
        var existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (product.isTrackInventory() && product.getStockQuantity() < newQuantity) {
                throw new InsufficientStockException(product.getName(), newQuantity, product.getStockQuantity());
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        log.info("Added {} x {} to cart for user {}", quantity, product.getName(), userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public CartDTO updateCartItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (quantity <= 0) {
            return removeFromCart(userId, productId);
        }

        if (product.isTrackInventory() && product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStockQuantity());
        }

        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProduct().getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        log.info("Updated cart item quantity to {} for product {} (user {})", quantity, product.getName(), userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public CartDTO removeFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);

        log.info("Removed product {} from cart for user {}", productId, userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public CartDTO clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clear();
        cartRepository.save(cart);

        log.info("Cleared cart for user {}", userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                Cart newCart = Cart.builder()
                    .user(user)
                    .build();
                return cartRepository.save(newCart);
            });
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
            .map(Cart::getTotalItems)
            .orElse(0);
    }

    @Transactional
    public void validateCartForCheckout(Long userId) {
        Cart cart = getOrCreateCart(userId);

        if (cart.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate each item's availability and stock
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Product '" + product.getName() + "' is no longer available");
            }
            if (product.isTrackInventory() && product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(product.getName(), item.getQuantity(), product.getStockQuantity());
            }
            // Update price in case it changed
            item.setPrice(product.getPrice());
        }

        cartRepository.save(cart);
    }

    @Transactional
    public CartDTO applyCoupon(Long userId, String couponCode) {
        Cart cart = getOrCreateCart(userId);

        if (cart.isEmpty()) {
            throw new BadRequestException("Cannot apply coupon to empty cart");
        }

        Coupon coupon = couponRepository.findByCodeAndActiveTrue(couponCode)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found or inactive"));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon is not valid or has expired");
        }

        if (!coupon.isApplicable(cart.getSubtotal())) {
            throw new BadRequestException("Order does not meet minimum amount for this coupon");
        }

        cart.setCoupon(coupon);
        cart.setCouponCode(couponCode);
        cart.setDiscountAmount(coupon.calculateDiscount(cart.getSubtotal()));

        cartRepository.save(cart);
        log.info("Applied coupon {} to cart for user {}", couponCode, userId);
        return CartDTO.fromEntity(cart);
    }

    @Transactional
    public CartDTO removeCoupon(Long userId) {
        Cart cart = getOrCreateCart(userId);

        cart.setCoupon(null);
        cart.setCouponCode(null);
        cart.setDiscountAmount(java.math.BigDecimal.ZERO);

        cartRepository.save(cart);
        log.info("Removed coupon from cart for user {}", userId);
        return CartDTO.fromEntity(cart);
    }
}
