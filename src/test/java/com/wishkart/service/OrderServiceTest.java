package com.wishkart.service;

import com.wishkart.dto.CheckoutRequest;
import com.wishkart.dto.OrderDTO;
import com.wishkart.entity.*;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private Order testOrder;

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

        CartItem cartItem = CartItem.builder()
            .product(testProduct)
            .quantity(2)
            .build();
        cartItem.setId(1L);

        testCart = Cart.builder()
            .user(testUser)
            .items(new ArrayList<>(Arrays.asList(cartItem)))
            .build();
        testCart.setId(1L);
        cartItem.setCart(testCart);

        OrderItem orderItem = OrderItem.builder()
            .product(testProduct)
            .productName(testProduct.getName())
            .quantity(2)
            .unitPrice(testProduct.getPrice())
            .build();
        orderItem.setId(1L);

        testOrder = Order.builder()
            .orderNumber("WK-20240101-0001")
            .user(testUser)
            .items(new ArrayList<>(Arrays.asList(orderItem)))
            .subtotal(new BigDecimal("199.98"))
            .shippingCost(new BigDecimal("9.99"))
            .taxAmount(new BigDecimal("20.00"))
            .totalAmount(new BigDecimal("229.97"))
            .status(Order.OrderStatus.PENDING)
            .paymentStatus(Order.PaymentStatus.PENDING)
            .shippingAddressLine1("123 Test St")
            .shippingCity("Test City")
            .shippingState("TS")
            .shippingPostalCode("12345")
            .shippingCountry("US")
            .build();
        testOrder.setId(1L);
        orderItem.setOrder(testOrder);
    }

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));

            OrderDTO result = orderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("WK-20240101-0001");
        }

        @Test
        @DisplayName("should throw exception when order not found")
        void shouldThrowExceptionWhenNotFound() {
            when(orderRepository.findByIdWithItems(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getOrderByNumber")
    class GetOrderByNumber {

        @Test
        @DisplayName("should return order when found by order number")
        void shouldReturnOrderWhenFoundByOrderNumber() {
            when(orderRepository.findByOrderNumberWithItems("WK-20240101-0001"))
                .thenReturn(Optional.of(testOrder));

            OrderDTO result = orderService.getOrderByNumber("WK-20240101-0001");

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("WK-20240101-0001");
        }
    }

    @Nested
    @DisplayName("getOrdersByUser")
    class GetOrdersByUser {

        @Test
        @DisplayName("should return paginated user orders")
        void shouldReturnPaginatedOrders() {
            Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));

            when(orderRepository.findByUserIdWithItems(anyLong(), any(Pageable.class)))
                .thenReturn(orderPage);

            Page<OrderDTO> result = orderService.getOrdersByUser(1L, Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("should update order status")
        void shouldUpdateOrderStatus() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            OrderDTO result = orderService.updateOrderStatus(1L, Order.OrderStatus.CONFIRMED);

            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw exception for invalid status transition")
        void shouldThrowExceptionForInvalidTransition() {
            testOrder.setStatus(Order.OrderStatus.DELIVERED);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, Order.OrderStatus.PENDING))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel pending order")
        void shouldCancelPendingOrder() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            OrderDTO result = orderService.cancelOrder(1L, "Customer request");

            assertThat(result).isNotNull();
            verify(productService).increaseStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("should throw exception when cancelling shipped order")
        void shouldThrowExceptionWhenCancellingShippedOrder() {
            testOrder.setStatus(Order.OrderStatus.SHIPPED);

            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.cancelOrder(1L, "Test"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel");
        }
    }

    @Nested
    @DisplayName("updatePaymentStatus")
    class UpdatePaymentStatus {

        @Test
        @DisplayName("should update payment status")
        void shouldUpdatePaymentStatus() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            OrderDTO result = orderService.updatePaymentStatus(1L, Order.PaymentStatus.COMPLETED, "pi_123");

            assertThat(result).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("addTrackingNumber")
    class AddTrackingNumber {

        @Test
        @DisplayName("should add tracking number to order")
        void shouldAddTrackingNumber() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            OrderDTO result = orderService.addTrackingNumber(1L, "1Z999AA10123456784");

            assertThat(result).isNotNull();
            verify(orderRepository).save(argThat(order ->
                "1Z999AA10123456784".equals(order.getTrackingNumber())
            ));
        }
    }

    @Nested
    @DisplayName("Analytics")
    class Analytics {

        @Test
        @DisplayName("should count orders by status")
        void shouldCountOrdersByStatus() {
            when(orderRepository.countByStatus(Order.OrderStatus.PENDING)).thenReturn(5L);

            long count = orderService.countOrdersByStatus(Order.OrderStatus.PENDING);

            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("should calculate total revenue")
        void shouldCalculateTotalRevenue() {
            when(orderRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("10000.00"));

            BigDecimal revenue = orderService.getTotalRevenue();

            assertThat(revenue).isEqualByComparingTo(new BigDecimal("10000.00"));
        }
    }
}
