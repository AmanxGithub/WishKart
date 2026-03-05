package com.wishkart.service;

import com.wishkart.dto.CheckoutRequest;
import com.wishkart.dto.OrderDTO;
import com.wishkart.entity.*;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CouponRepository;
import com.wishkart.repository.OrderRepository;
import com.wishkart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for order management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public OrderDTO createOrder(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate cart
        cartService.validateCartForCheckout(userId);
        Cart cart = cartService.getOrCreateCart(userId);

        // Create order
        Order order = Order.builder()
            .user(user)
            .status(Order.OrderStatus.PENDING)
            .paymentStatus(Order.PaymentStatus.PENDING)
            .paymentMethod(Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
            .shippingName(request.getShippingName())
            .shippingAddressLine1(request.getShippingAddressLine1())
            .shippingAddressLine2(request.getShippingAddressLine2())
            .shippingCity(request.getShippingCity())
            .shippingState(request.getShippingState())
            .shippingPostalCode(request.getShippingPostalCode())
            .shippingCountry(request.getShippingCountry())
            .shippingPhone(request.getShippingPhone())
            .billingSameAsShipping(request.isBillingSameAsShipping())
            .customerNotes(request.getCustomerNotes())
            .build();

        // Set billing address if different
        if (!request.isBillingSameAsShipping()) {
            order.setBillingAddressLine1(request.getBillingAddressLine1());
            order.setBillingCity(request.getBillingCity());
            order.setBillingState(request.getBillingState());
            order.setBillingPostalCode(request.getBillingPostalCode());
            order.setBillingCountry(request.getBillingCountry());
        }

        // Calculate totals first (before adding items, use cart values)
        order.setSubtotal(cart.getSubtotal());
        order.setShippingCost(calculateShippingCost(cart));
        order.setTaxAmount(calculateTax(cart.getSubtotal()));

        // Apply coupon if provided
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            applyCoupon(order, request.getCouponCode());
        }

        order.calculateTotals();

        // Save order FIRST to persist it (so it's no longer transient)
        order = orderRepository.save(order);

        // Now add order items from cart (order is now persistent)
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);
        }

        // Save again to persist the order items
        order = orderRepository.save(order);

        // Decrease stock for each product
        for (OrderItem item : order.getItems()) {
            productService.decreaseStock(item.getProduct().getId(), item.getQuantity());
        }

        // Clear cart
        cartService.clearCart(userId);

        log.info("Order created: {} for user {}", order.getOrderNumber(), user.getEmail());
        return OrderDTO.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return OrderDTO.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return OrderDTO.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdWithItems(userId, pageable)
            .map(OrderDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
            .map(OrderDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
            .map(OrderDTO::fromEntity);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Order.OrderStatus currentStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);

        // Update timestamps based on status
        switch (newStatus) {
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> order.setCancelledAt(LocalDateTime.now());
        }

        order = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
        return OrderDTO.fromEntity(order);
    }

    @Transactional
    public OrderDTO updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus, String paymentIntentId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setPaymentStatus(paymentStatus);
        if (paymentIntentId != null) {
            order.setPaymentIntentId(paymentIntentId);
        }

        if (paymentStatus == Order.PaymentStatus.COMPLETED) {
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(Order.OrderStatus.CONFIRMED);
        }

        order = orderRepository.save(order);
        log.info("Order {} payment status updated to {}", order.getOrderNumber(), paymentStatus);
        return OrderDTO.fromEntity(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
            order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel order that has been shipped or delivered");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            productService.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order = orderRepository.save(order);
        log.info("Order {} cancelled. Reason: {}", order.getOrderNumber(), reason);
        return OrderDTO.fromEntity(order);
    }

    @Transactional
    public OrderDTO addTrackingNumber(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setTrackingNumber(trackingNumber);
        order = orderRepository.save(order);
        log.info("Tracking number {} added to order {}", trackingNumber, order.getOrderNumber());
        return OrderDTO.fromEntity(order);
    }

    private void applyCoupon(Order order, String couponCode) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode)
            .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        if (!coupon.isApplicable(order.getSubtotal())) {
            throw new BadRequestException("Coupon is not applicable to this order");
        }

        BigDecimal discount = coupon.calculateDiscount(order.getSubtotal());
        order.setCouponCode(couponCode);
        order.setDiscountAmount(discount);
        coupon.incrementUsage();
        couponRepository.save(coupon);
    }

    private BigDecimal calculateShippingCost(Cart cart) {
        // Simple shipping calculation - can be enhanced
        BigDecimal subtotal = cart.getSubtotal();
        if (subtotal.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return BigDecimal.ZERO; // Free shipping over $100
        }
        return BigDecimal.valueOf(9.99);
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simple tax calculation (8.5%) - can be enhanced with location-based tax
        return subtotal.multiply(BigDecimal.valueOf(0.085))
            .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(Order.OrderStatus from, Order.OrderStatus to) {
        // Define valid status transitions
        boolean valid = switch (from) {
            case PENDING -> to == Order.OrderStatus.CONFIRMED || to == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> to == Order.OrderStatus.PROCESSING || to == Order.OrderStatus.CANCELLED;
            case PROCESSING -> to == Order.OrderStatus.SHIPPED || to == Order.OrderStatus.CANCELLED;
            case SHIPPED -> to == Order.OrderStatus.DELIVERED;
            case DELIVERED -> to == Order.OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };

        if (!valid) {
            throw new BadRequestException("Invalid status transition from " + from + " to " + to);
        }
    }

    // Analytics methods
    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public long countOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countOrdersToday() {
        return orderRepository.countOrdersSince(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = orderRepository.calculateRevenueBetweenDates(start, end);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findAll(Pageable.ofSize(limit)).getContent();
    }
}
