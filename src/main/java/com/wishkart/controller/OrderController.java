package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.CheckoutRequest;
import com.wishkart.dto.OrderDTO;
import com.wishkart.dto.PagedResponse;
import com.wishkart.service.OrderService;
import com.wishkart.service.PaymentService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for order endpoints.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management operations")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @Operation(summary = "Create order from cart")
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(@Valid @RequestBody CheckoutRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        OrderDTO order = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDTO>>> getMyOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        Page<OrderDTO> orders = orderService.getOrdersByUser(userId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(orders)));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByNumber(@PathVariable("orderNumber") String orderNumber) {
        OrderDTO order = orderService.getOrderByNumber(orderNumber);
        
        // Verify the order belongs to the current user (unless admin)
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        if (!order.getUserId().equals(userId) && !SecurityUtil.isAdmin()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable("orderId") Long orderId,
            @RequestParam(name = "reason", required = false) String reason) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Verify order ownership
        OrderDTO existingOrder = orderService.getOrderById(orderId);
        if (!existingOrder.getUserId().equals(userId) && !SecurityUtil.isAdmin()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        
        OrderDTO order = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    @PostMapping("/{orderId}/payment-intent")
    @Operation(summary = "Create payment intent for order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentIntent(@PathVariable("orderId") Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Verify order ownership
        OrderDTO existingOrder = orderService.getOrderById(orderId);
        if (!existingOrder.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        
        Map<String, Object> paymentIntent = paymentService.createPaymentIntent(orderId);
        return ResponseEntity.ok(ApiResponse.success(paymentIntent));
    }

    @PostMapping("/payment/confirm")
    @Operation(summary = "Confirm payment")
    public ResponseEntity<ApiResponse<OrderDTO>> confirmPayment(@RequestParam("paymentIntentId") String paymentIntentId) {
        OrderDTO order = paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", order));
    }
}
