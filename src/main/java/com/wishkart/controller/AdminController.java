package com.wishkart.controller;

import com.wishkart.dto.*;
import com.wishkart.entity.Order;
import com.wishkart.entity.User;
import com.wishkart.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for admin dashboard endpoints.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User stats
        stats.put("totalUsers", userService.countUsers());
        stats.put("totalCustomers", userService.countCustomers());
        stats.put("totalAdmins", userService.countAdmins());

        // Product stats
        stats.put("totalProducts", productService.countActiveProducts());
        stats.put("lowStockProducts", productService.countLowStockProducts());

        // Category stats
        stats.put("totalCategories", categoryService.countCategories());

        // Order stats
        stats.put("totalOrders", orderService.countOrders());
        stats.put("pendingOrders", orderService.countOrdersByStatus(Order.OrderStatus.PENDING));
        stats.put("processingOrders", orderService.countOrdersByStatus(Order.OrderStatus.PROCESSING));
        stats.put("ordersToday", orderService.countOrdersToday());

        // Revenue
        stats.put("totalRevenue", orderService.getTotalRevenue());
        stats.put("revenueToday", orderService.getRevenueForPeriod(
            LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
            LocalDateTime.now()
        ));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        Page<UserDTO> users;
        if (search != null && !search.isBlank()) {
            users = userService.searchUsers(search, PageRequest.of(page, size));
        } else {
            users = userService.getAllUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(users)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{id}/toggle-status")
    @Operation(summary = "Enable/disable user")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated"));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        userService.changeUserRole(id, User.Role.valueOf(role.toUpperCase()));
        return ResponseEntity.ok(ApiResponse.success("User role updated"));
    }

    // ==================== PRODUCT MANAGEMENT ====================

    @GetMapping("/products")
    @Operation(summary = "Get all products (including inactive)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProductsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ProductDTO> products = productService.getAllProductsAdmin(
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @PostMapping("/products")
    @Operation(summary = "Create new product")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDTO product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Product created", product));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDTO product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", product));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted"));
    }

    @PutMapping("/products/{id}/stock")
    @Operation(summary = "Update product stock")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        productService.updateStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated"));
    }

    @GetMapping("/products/low-stock")
    @Operation(summary = "Get low stock products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getLowStockProducts() {
        List<ProductDTO> products = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/out-of-stock")
    @Operation(summary = "Get out of stock products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getOutOfStockProducts() {
        List<ProductDTO> products = productService.getOutOfStockProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // ==================== CATEGORY MANAGEMENT ====================

    @PostMapping("/categories")
    @Operation(summary = "Create new category")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer displayOrder) {
        CategoryDTO category = categoryService.createCategory(name, description, imageUrl, parentId, displayOrder);
        return ResponseEntity.ok(ApiResponse.success("Category created", category));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) Boolean active) {
        CategoryDTO category = categoryService.updateCategory(id, name, description, imageUrl, parentId, displayOrder, active);
        return ResponseEntity.ok(ApiResponse.success("Category updated", category));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted"));
    }

    // ==================== ORDER MANAGEMENT ====================

    @GetMapping("/orders")
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Page<OrderDTO> orders;
        if (status != null && !status.isBlank()) {
            orders = orderService.getOrdersByStatus(
                Order.OrderStatus.valueOf(status.toUpperCase()),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
            );
        } else {
            orders = orderService.getAllOrders(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
            );
        }
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(orders)));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get order details")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        OrderDTO order = orderService.updateOrderStatus(id, Order.OrderStatus.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @PutMapping("/orders/{id}/tracking")
    @Operation(summary = "Add tracking number to order")
    public ResponseEntity<ApiResponse<OrderDTO>> addTrackingNumber(
            @PathVariable Long id,
            @RequestParam String trackingNumber) {
        OrderDTO order = orderService.addTrackingNumber(id, trackingNumber);
        return ResponseEntity.ok(ApiResponse.success("Tracking number added", order));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        OrderDTO order = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    // ==================== ANALYTICS ====================

    @GetMapping("/analytics/revenue")
    @Operation(summary = "Get revenue analytics")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getRevenueAnalytics(
            @RequestParam(required = false) String period) {

        Map<String, BigDecimal> analytics = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Today
        analytics.put("today", orderService.getRevenueForPeriod(
            now.withHour(0).withMinute(0).withSecond(0), now));

        // This week
        analytics.put("thisWeek", orderService.getRevenueForPeriod(
            now.minusDays(7), now));

        // This month
        analytics.put("thisMonth", orderService.getRevenueForPeriod(
            now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0), now));

        // Total
        analytics.put("total", orderService.getTotalRevenue());

        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
