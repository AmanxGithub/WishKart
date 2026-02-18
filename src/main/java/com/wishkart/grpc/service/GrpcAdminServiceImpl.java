package com.wishkart.grpc.service;

import com.wishkart.dto.CategoryDTO;
import com.wishkart.dto.OrderDTO;
import com.wishkart.dto.PagedResponse;
import com.wishkart.dto.ProductDTO;
import com.wishkart.dto.ReviewDTO;
import com.wishkart.dto.UserDTO;
import com.wishkart.entity.Order;
import com.wishkart.entity.User;
import com.wishkart.grpc.admin.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * gRPC service implementation for Admin operations.
 * All methods require admin role.
 * Delegates to existing services for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcAdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    // ==================== Dashboard ====================

    @Override
    public void getDashboardStats(Empty request,
                                  StreamObserver<DashboardStatsResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetDashboardStats request");

            DashboardStatsResponse response = DashboardStatsResponse.newBuilder()
                .setSuccess(true)
                .setTotalUsers(userService.countUsers())
                .setTotalCustomers(userService.countCustomers())
                .setTotalAdmins(userService.countAdmins())
                .setTotalProducts(productService.countActiveProducts())
                .setLowStockProducts(productService.countLowStockProducts())
                .setTotalCategories(categoryService.countCategories())
                .setTotalOrders(orderService.countOrders())
                .setPendingOrders(orderService.countOrdersByStatus(Order.OrderStatus.PENDING))
                .setProcessingOrders(orderService.countOrdersByStatus(Order.OrderStatus.PROCESSING))
                .setOrdersToday(orderService.countOrdersToday())
                .setTotalRevenue(orderService.getTotalRevenue().toString())
                .setRevenueToday(orderService.getRevenueForPeriod(
                    LocalDate.now().atStartOfDay(),
                    LocalDate.now().plusDays(1).atStartOfDay()).toString())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== User Management ====================

    @Override
    public void getAllUsers(GetUsersRequest request,
                            StreamObserver<UserListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetAllUsers request");

            PageRequest pageRequest = buildPageRequest(request.getPagination());
            Page<UserDTO> users;

            if (request.getSearch() != null && !request.getSearch().isEmpty()) {
                users = userService.searchUsers(request.getSearch(), pageRequest);
            } else {
                users = userService.getAllUsers(pageRequest);
            }

            UserListResponse.Builder builder = UserListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toAdminPaginationInfo(users));

            users.getContent().forEach(u -> builder.addUsers(ProtoConverter.toAdminUserInfo(u)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getUserById(UserIdRequest request,
                            StreamObserver<UserResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetUserById request for user: {}", request.getId());

            UserDTO user = userService.getUserById(request.getId());

            UserResponse response = UserResponse.newBuilder()
                .setSuccess(true)
                .setUser(ProtoConverter.toAdminUserInfo(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void toggleUserStatus(UserIdRequest request,
                                 StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC ToggleUserStatus request for user: {}", request.getId());

            userService.toggleUserStatus(request.getId());

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "User status toggled");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void changeUserRole(ChangeUserRoleRequest request,
                               StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC ChangeUserRole request for user: {}, role: {}",
                request.getId(), request.getRole());

            User.Role role = User.Role.valueOf(request.getRole().toUpperCase());
            userService.changeUserRole(request.getId(), role);

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "User role changed");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== Product Management ====================

    @Override
    public void getAllProductsAdmin(PaginationRequest request,
                                    StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetAllProductsAdmin request");

            PageRequest pageRequest = buildPageRequest(request);
            Page<ProductDTO> products = productService.getAllProductsAdmin(pageRequest);

            ProductListResponse.Builder builder = ProductListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toAdminPaginationInfo(products));

            products.getContent().forEach(p -> builder.addProducts(ProtoConverter.toAdminProduct(p)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void createProduct(CreateProductRequest request,
                              StreamObserver<ProductResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC CreateProduct request for product: {}", request.getName());

            // Convert gRPC request to DTO
            com.wishkart.dto.CreateProductRequest createRequest = new com.wishkart.dto.CreateProductRequest();
            createRequest.setName(request.getName());
            createRequest.setSku(request.getSku());
            createRequest.setDescription(request.getDescription());
            createRequest.setShortDescription(request.getShortDescription());
            createRequest.setPrice(ProtoConverter.stringToBigDecimal(request.getPrice()));
            createRequest.setCompareAtPrice(ProtoConverter.stringToBigDecimal(request.getCompareAtPrice()));
            createRequest.setCostPrice(ProtoConverter.stringToBigDecimal(request.getCostPrice()));
            createRequest.setStockQuantity(request.getStockQuantity());
            createRequest.setLowStockThreshold(request.getLowStockThreshold());
            createRequest.setTrackInventory(request.getTrackInventory());
            createRequest.setActive(request.getActive());
            createRequest.setFeatured(request.getFeatured());
            createRequest.setWeight(ProtoConverter.stringToBigDecimal(request.getWeight()));
            createRequest.setWeightUnit(request.getWeightUnit());
            createRequest.setCategoryId(request.getCategoryId() > 0 ? request.getCategoryId() : null);
            createRequest.setImageUrls(request.getImageUrlsList());
            createRequest.setMetaTitle(request.getMetaTitle());
            createRequest.setMetaDescription(request.getMetaDescription());

            ProductDTO product = productService.createProduct(createRequest);

            ProductResponse response = ProtoConverter.toAdminProductResponse(product, "Product created successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateProduct(UpdateProductRequest request,
                              StreamObserver<ProductResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC UpdateProduct request for product: {}", request.getId());

            CreateProductRequest productData = request.getProduct();

            // Convert gRPC request to DTO
            com.wishkart.dto.CreateProductRequest updateRequest = new com.wishkart.dto.CreateProductRequest();
            updateRequest.setName(productData.getName());
            updateRequest.setSku(productData.getSku());
            updateRequest.setDescription(productData.getDescription());
            updateRequest.setShortDescription(productData.getShortDescription());
            updateRequest.setPrice(ProtoConverter.stringToBigDecimal(productData.getPrice()));
            updateRequest.setCompareAtPrice(ProtoConverter.stringToBigDecimal(productData.getCompareAtPrice()));
            updateRequest.setCostPrice(ProtoConverter.stringToBigDecimal(productData.getCostPrice()));
            updateRequest.setStockQuantity(productData.getStockQuantity());
            updateRequest.setLowStockThreshold(productData.getLowStockThreshold());
            updateRequest.setTrackInventory(productData.getTrackInventory());
            updateRequest.setActive(productData.getActive());
            updateRequest.setFeatured(productData.getFeatured());
            updateRequest.setWeight(ProtoConverter.stringToBigDecimal(productData.getWeight()));
            updateRequest.setWeightUnit(productData.getWeightUnit());
            updateRequest.setCategoryId(productData.getCategoryId() > 0 ? productData.getCategoryId() : null);
            updateRequest.setImageUrls(productData.getImageUrlsList());
            updateRequest.setMetaTitle(productData.getMetaTitle());
            updateRequest.setMetaDescription(productData.getMetaDescription());

            ProductDTO product = productService.updateProduct(request.getId(), updateRequest);

            ProductResponse response = ProtoConverter.toAdminProductResponse(product, "Product updated successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void deleteProduct(ProductIdRequest request,
                              StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC DeleteProduct request for product: {}", request.getId());

            productService.deleteProduct(request.getId());

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "Product deleted successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateStock(UpdateStockRequest request,
                            StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC UpdateStock request for product: {}", request.getProductId());

            productService.updateStock(request.getProductId(), request.getQuantity());

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "Stock updated successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getLowStockProducts(Empty request,
                                    StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetLowStockProducts request");

            List<ProductDTO> products = productService.getLowStockProducts();

            ProductListResponse.Builder builder = ProductListResponse.newBuilder()
                .setSuccess(true);

            products.forEach(p -> builder.addProducts(ProtoConverter.toAdminProduct(p)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getOutOfStockProducts(Empty request,
                                      StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetOutOfStockProducts request");

            List<ProductDTO> products = productService.getOutOfStockProducts();

            ProductListResponse.Builder builder = ProductListResponse.newBuilder()
                .setSuccess(true);

            products.forEach(p -> builder.addProducts(ProtoConverter.toAdminProduct(p)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== Category Management ====================

    @Override
    public void createCategory(com.wishkart.grpc.admin.CreateCategoryRequest request,
                               StreamObserver<CategoryResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC CreateCategory request for category: {}", request.getName());

            CategoryDTO category = categoryService.createCategory(
                request.getName(),
                request.getDescription(),
                request.getImageUrl(),
                request.getParentId() > 0 ? request.getParentId() : null,
                request.getDisplayOrder()
            );

            CategoryResponse response = ProtoConverter.toAdminCategoryResponse(category, "Category created successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateCategory(UpdateCategoryRequest request,
                               StreamObserver<CategoryResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC UpdateCategory request for category: {}", request.getId());

            CategoryDTO category = categoryService.updateCategory(
                request.getId(),
                request.getName(),
                request.getDescription(),
                request.getImageUrl(),
                request.getParentId() > 0 ? request.getParentId() : null,
                request.getDisplayOrder(),
                request.getActive()
            );

            CategoryResponse response = ProtoConverter.toAdminCategoryResponse(category, "Category updated successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void deleteCategory(CategoryIdRequest request,
                               StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC DeleteCategory request for category: {}", request.getId());

            categoryService.deleteCategory(request.getId());

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "Category deleted successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== Order Management ====================

    @Override
    public void getAllOrders(GetOrdersRequest request,
                             StreamObserver<OrderListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetAllOrders request");

            PageRequest pageRequest = buildPageRequest(request.getPagination());
            Page<OrderDTO> orders;

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                Order.OrderStatus status = Order.OrderStatus.valueOf(request.getStatus().toUpperCase());
                orders = orderService.getOrdersByStatus(status, pageRequest);
            } else {
                orders = orderService.getAllOrders(pageRequest);
            }

            OrderListResponse.Builder builder = OrderListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toAdminPaginationInfo(orders));

            orders.getContent().forEach(o -> builder.addOrders(ProtoConverter.toAdminOrder(o)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getOrderById(OrderIdRequest request,
                             StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetOrderById (Admin) request for order: {}", request.getId());

            OrderDTO order = orderService.getOrderById(request.getId());

            OrderResponse response = ProtoConverter.toAdminOrderResponse(order, "Order retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateOrderStatus(UpdateOrderStatusRequest request,
                                  StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC UpdateOrderStatus request for order: {}, status: {}",
                request.getId(), request.getStatus());

            Order.OrderStatus status = Order.OrderStatus.valueOf(request.getStatus().toUpperCase());
            OrderDTO order = orderService.updateOrderStatus(request.getId(), status);

            OrderResponse response = ProtoConverter.toAdminOrderResponse(order, "Order status updated");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void addTrackingNumber(AddTrackingRequest request,
                                  StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC AddTrackingNumber request for order: {}", request.getId());

            OrderDTO order = orderService.addTrackingNumber(request.getId(), request.getTrackingNumber());

            OrderResponse response = ProtoConverter.toAdminOrderResponse(order, "Tracking number added");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void adminCancelOrder(AdminCancelOrderRequest request,
                                 StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC AdminCancelOrder request for order: {}", request.getId());

            OrderDTO order = orderService.cancelOrder(request.getId(), request.getReason());

            OrderResponse response = ProtoConverter.toAdminOrderResponse(order, "Order cancelled");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== Analytics ====================

    @Override
    public void getRevenueAnalytics(AnalyticsPeriodRequest request,
                                    StreamObserver<RevenueAnalyticsResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetRevenueAnalytics request");

            LocalDate today = LocalDate.now();
            BigDecimal todayRevenue = orderService.getRevenueForPeriod(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
            BigDecimal weekRevenue = orderService.getRevenueForPeriod(
                today.minusWeeks(1).atStartOfDay(),
                today.plusDays(1).atStartOfDay());
            BigDecimal monthRevenue = orderService.getRevenueForPeriod(
                today.minusMonths(1).atStartOfDay(),
                today.plusDays(1).atStartOfDay());
            BigDecimal totalRevenue = orderService.getTotalRevenue();

            RevenueAnalyticsResponse response = RevenueAnalyticsResponse.newBuilder()
                .setSuccess(true)
                .setToday(todayRevenue.toString())
                .setThisWeek(weekRevenue.toString())
                .setThisMonth(monthRevenue.toString())
                .setTotal(totalRevenue.toString())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // ==================== Review Moderation ====================

    @Override
    public void getPendingReviews(PaginationRequest request,
                                  StreamObserver<PendingReviewsResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC GetPendingReviews request");

            int page = request != null && request.getPage() >= 0 ? request.getPage() : 0;
            int size = request != null && request.getSize() > 0 ? request.getSize() : 10;

            PagedResponse<ReviewDTO> reviews = reviewService.getPendingReviews(page, size);

            PendingReviewsResponse.Builder builder = PendingReviewsResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toAdminPaginationInfo(reviews));

            reviews.getContent().forEach(r -> builder.addReviews(ProtoConverter.toAdminReview(r)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void moderateReview(ModerateReviewRequest request,
                               StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC ModerateReview request for review: {}, approved: {}",
                request.getReviewId(), request.getApproved());

            reviewService.moderateReview(request.getReviewId(), request.getApproved());

            String message = request.getApproved() ? "Review approved" : "Review rejected";
            ApiResponse response = ProtoConverter.toAdminApiResponse(true, message);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void adminDeleteReview(ReviewIdRequest request,
                                  StreamObserver<ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAdmin();
            log.debug("gRPC AdminDeleteReview request for review: {}", request.getReviewId());

            reviewService.adminDeleteReview(request.getReviewId());

            ApiResponse response = ProtoConverter.toAdminApiResponse(true, "Review deleted");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Helper methods

    private PageRequest buildPageRequest(PaginationRequest pagination) {
        if (pagination == null) {
            return PageRequest.of(0, 10, Sort.by("createdAt").descending());
        }

        int page = pagination.getPage() >= 0 ? pagination.getPage() : 0;
        int size = pagination.getSize() > 0 ? pagination.getSize() : 10;

        if (pagination.getSortBy() != null && !pagination.getSortBy().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(pagination.getSortDirection())
                ? Sort.by(pagination.getSortBy()).descending()
                : Sort.by(pagination.getSortBy()).ascending();
            return PageRequest.of(page, size, sort);
        }

        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}
