package com.wishkart.service;

import com.wishkart.dto.CartDTO;
import com.wishkart.entity.*;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.InsufficientStockException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CartItemRepository;
import com.wishkart.repository.CartRepository;
import com.wishkart.repository.CouponRepository;
import com.wishkart.repository.ProductRepository;
import com.wishkart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .build();
        testUser.setId(1L);

        testProduct = Product.builder()
            .name("Test Product")
            .slug("test-product")
            .sku("SKU-001")
            .price(new BigDecimal("99.99"))
            .stockQuantity(100)
            .active(true)
            .trackInventory(true)
            .build();
        testProduct.setId(1L);

        testCart = Cart.builder()
            .user(testUser)
            .items(new ArrayList<>())
            .build();
        testCart.setId(1L);
    }

    private CartItem createCartItem(Long id, Product product, int quantity) {
        CartItem item = CartItem.builder()
            .cart(testCart)
            .product(product)
            .quantity(quantity)
            .price(product.getPrice())
            .build();
        item.setId(id);
        return item;
    }

    private Coupon createCoupon(Long id, String code, Coupon.DiscountType type, BigDecimal value) {
        Coupon coupon = Coupon.builder()
            .code(code)
            .discountType(type)
            .discountValue(value)
            .minimumOrderAmount(BigDecimal.ZERO)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(30))
            .active(true)
            .build();
        coupon.setId(id);
        return coupon;
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("should return cart for user")
        void shouldReturnCartForUser() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));

            CartDTO result = cartService.getCart(1L);

            assertThat(result).isNotNull();
            assertThat(result.getTotalItems()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create new cart if not exists")
        void shouldCreateNewCartIfNotExists() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartDTO result = cartService.getCart(1L);

            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCart(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addToCart")
    class AddToCart {

        @Test
        @DisplayName("should add new item to cart")
        void shouldAddNewItemToCart() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

            CartDTO result = cartService.addToCart(1L, 1L, 2);

            assertThat(result).isNotNull();
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("should update quantity if item already in cart")
        void shouldUpdateQuantityIfItemExists() {
            CartItem existingItem = createCartItem(1L, testProduct, 1);
            testCart.getItems().add(existingItem);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

            CartDTO result = cartService.addToCart(1L, 1L, 2);

            assertThat(result).isNotNull();
            assertThat(existingItem.getQuantity()).isEqualTo(3); // 1 + 2
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(1L, 999L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when insufficient stock")
        void shouldThrowExceptionWhenInsufficientStock() {
            testProduct.setStockQuantity(5);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            assertThatThrownBy(() -> cartService.addToCart(1L, 1L, 10))
                .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("should throw exception when product is inactive")
        void shouldThrowExceptionWhenProductInactive() {
            testProduct.setActive(false);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            assertThatThrownBy(() -> cartService.addToCart(1L, 1L, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("updateCartItemQuantity")
    class UpdateCartItemQuantity {

        @Test
        @DisplayName("should update item quantity")
        void shouldUpdateItemQuantity() {
            CartItem existingItem = createCartItem(1L, testProduct, 2);
            testCart.getItems().add(existingItem);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));

            CartDTO result = cartService.updateCartItemQuantity(1L, 1L, 5);

            assertThat(result).isNotNull();
            assertThat(existingItem.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("should remove item when quantity is zero or less")
        void shouldRemoveItemWhenQuantityZero() {
            CartItem existingItem = createCartItem(1L, testProduct, 2);
            testCart.getItems().add(existingItem);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartDTO result = cartService.updateCartItemQuantity(1L, 1L, 0);

            assertThat(result).isNotNull();
            // removeFromCart is called, which saves the cart after modifying items
            verify(cartRepository, atLeastOnce()).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("removeFromCart")
    class RemoveFromCart {

        @Test
        @DisplayName("should remove item from cart")
        void shouldRemoveItemFromCart() {
            CartItem existingItem = createCartItem(1L, testProduct, 2);
            testCart.getItems().add(existingItem);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartDTO result = cartService.removeFromCart(1L, 1L);

            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("should clear all items from cart")
        void shouldClearAllItems() {
            CartItem item1 = createCartItem(1L, testProduct, 1);
            testCart.getItems().add(item1);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            cartService.clearCart(1L);

            verify(cartRepository).save(any(Cart.class));
            assertThat(testCart.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("applyCoupon")
    class ApplyCoupon {

        @Test
        @DisplayName("should apply valid coupon")
        void shouldApplyValidCoupon() {
            CartItem item = createCartItem(1L, testProduct, 2);
            testCart.getItems().add(item);

            Coupon coupon = createCoupon(1L, "SAVE10", Coupon.DiscountType.PERCENTAGE, new BigDecimal("10"));

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeAndActiveTrue("SAVE10")).thenReturn(Optional.of(coupon));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartDTO result = cartService.applyCoupon(1L, "SAVE10");

            assertThat(result).isNotNull();
            assertThat(testCart.getCoupon()).isEqualTo(coupon);
        }

        @Test
        @DisplayName("should throw exception for empty cart")
        void shouldThrowExceptionForEmptyCart() {
            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));

            assertThatThrownBy(() -> cartService.applyCoupon(1L, "SAVE10"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty cart");
        }

        @Test
        @DisplayName("should throw exception for invalid coupon")
        void shouldThrowExceptionForInvalidCoupon() {
            CartItem item = createCartItem(1L, testProduct, 1);
            testCart.getItems().add(item);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeAndActiveTrue("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.applyCoupon(1L, "INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception for expired coupon")
        void shouldThrowExceptionForExpiredCoupon() {
            CartItem item = createCartItem(1L, testProduct, 1);
            testCart.getItems().add(item);

            Coupon expiredCoupon = Coupon.builder()
                .code("EXPIRED")
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().minusDays(1))
                .active(true)
                .build();
            expiredCoupon.setId(1L);

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(couponRepository.findByCodeAndActiveTrue("EXPIRED")).thenReturn(Optional.of(expiredCoupon));

            assertThatThrownBy(() -> cartService.applyCoupon(1L, "EXPIRED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not valid");
        }
    }

    @Nested
    @DisplayName("removeCoupon")
    class RemoveCoupon {

        @Test
        @DisplayName("should remove coupon from cart")
        void shouldRemoveCouponFromCart() {
            Coupon coupon = Coupon.builder()
                .code("SAVE10")
                .build();
            coupon.setId(1L);
            testCart.setCoupon(coupon);
            testCart.setCouponCode("SAVE10");

            when(cartRepository.findByUserIdWithItems(1L)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            CartDTO result = cartService.removeCoupon(1L);

            assertThat(result).isNotNull();
            assertThat(testCart.getCoupon()).isNull();
            assertThat(testCart.getCouponCode()).isNull();
        }
    }

    @Nested
    @DisplayName("getCartItemCount")
    class GetCartItemCount {

        @Test
        @DisplayName("should return total item count")
        void shouldReturnTotalItemCount() {
            CartItem item1 = createCartItem(1L, testProduct, 2);
            CartItem item2 = createCartItem(2L, testProduct, 3);
            testCart.getItems().add(item1);
            testCart.getItems().add(item2);

            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

            int count = cartService.getCartItemCount(1L);

            assertThat(count).isEqualTo(5); // 2 + 3
        }

        @Test
        @DisplayName("should return zero when cart not found")
        void shouldReturnZeroWhenCartNotFound() {
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

            int count = cartService.getCartItemCount(1L);

            assertThat(count).isEqualTo(0);
        }
    }
}
