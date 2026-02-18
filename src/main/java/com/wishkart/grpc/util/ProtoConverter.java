package com.wishkart.grpc.util;

import com.google.protobuf.Timestamp;
import com.wishkart.dto.*;
import com.wishkart.grpc.auth.AuthResponse;
import com.wishkart.grpc.auth.UserInfo;
import com.wishkart.grpc.cart.Cart;
import com.wishkart.grpc.cart.CartItem;
import com.wishkart.grpc.cart.CartResponse;
import com.wishkart.grpc.category.Category;
import com.wishkart.grpc.category.CategoryResponse;
import com.wishkart.grpc.order.Order;
import com.wishkart.grpc.order.OrderItem;
import com.wishkart.grpc.order.OrderResponse;
import com.wishkart.grpc.product.Product;
import com.wishkart.grpc.product.ProductResponse;
import com.wishkart.grpc.review.Review;
import com.wishkart.grpc.review.ReviewStats;
import com.wishkart.grpc.wishlist.Wishlist;
import com.wishkart.grpc.wishlist.WishlistProduct;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Utility class for converting between DTOs and Proto messages.
 */
public final class ProtoConverter {

    private ProtoConverter() {}

    // ==================== Product ====================

    public static Product toProto(ProductDTO dto) {
        if (dto == null) return Product.getDefaultInstance();

        Product.Builder builder = Product.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setName(nullSafe(dto.getName()))
            .setSku(nullSafe(dto.getSku()))
            .setSlug(nullSafe(dto.getSlug()))
            .setDescription(nullSafe(dto.getDescription()))
            .setShortDescription(nullSafe(dto.getShortDescription()))
            .setPrice(bigDecimalToString(dto.getPrice()))
            .setCompareAtPrice(bigDecimalToString(dto.getCompareAtPrice()))
            .setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
            .setInStock(dto.isInStock())
            .setLowStock(dto.isLowStock())
            .setActive(dto.isActive())
            .setFeatured(dto.isFeatured())
            .setWeight(bigDecimalToString(dto.getWeight()))
            .setWeightUnit(nullSafe(dto.getWeightUnit()))
            .setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : 0)
            .setCategoryName(nullSafe(dto.getCategoryName()))
            .setPrimaryImage(nullSafe(dto.getPrimaryImage()))
            .setPrimaryImageUrl(nullSafe(dto.getPrimaryImageUrl()))
            .setAverageRating(dto.getAverageRating())
            .setReviewCount(dto.getReviewCount())
            .setDiscountPercentage(bigDecimalToString(dto.getDiscountPercentage()));

        if (dto.getImages() != null) {
            builder.addAllImages(dto.getImages());
        }

        return builder.build();
    }

    public static ProductResponse toProductResponse(ProductDTO dto, String message) {
        return ProductResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setProduct(toProto(dto))
            .setTimestamp(LocalDateTime.now().toString())
            .build();
    }

    // ==================== Category ====================

    public static Category toProto(CategoryDTO dto) {
        if (dto == null) return Category.getDefaultInstance();

        Category.Builder builder = Category.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setName(nullSafe(dto.getName()))
            .setSlug(nullSafe(dto.getSlug()))
            .setDescription(nullSafe(dto.getDescription()))
            .setImageUrl(nullSafe(dto.getImageUrl()))
            .setActive(dto.isActive())
            .setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
            .setParentId(dto.getParentId() != null ? dto.getParentId() : 0)
            .setParentName(nullSafe(dto.getParentName()))
            .setProductCount(dto.getProductCount());

        if (dto.getSubcategories() != null) {
            dto.getSubcategories().forEach(sub -> builder.addSubcategories(toProto(sub)));
        }

        return builder.build();
    }

    public static CategoryResponse toCategoryResponse(CategoryDTO dto, String message) {
        return CategoryResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setCategory(toProto(dto))
            .build();
    }

    // ==================== Cart ====================

    public static Cart toProto(CartDTO dto) {
        if (dto == null) return Cart.getDefaultInstance();

        Cart.Builder builder = Cart.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setCouponCode(nullSafe(dto.getCouponCode()))
            .setDiscountAmount(bigDecimalToString(dto.getDiscountAmount()))
            .setSubtotal(bigDecimalToString(dto.getSubtotal()))
            .setTotal(bigDecimalToString(dto.getTotal()))
            .setTotalItems(dto.getTotalItems())
            .setUniqueItemCount(dto.getUniqueItemCount());

        if (dto.getItems() != null) {
            dto.getItems().forEach(item -> builder.addItems(toProto(item)));
        }

        return builder.build();
    }

    public static CartItem toProto(CartItemDTO dto) {
        if (dto == null) return CartItem.getDefaultInstance();

        return CartItem.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setProductId(dto.getProductId() != null ? dto.getProductId() : 0)
            .setProductName(nullSafe(dto.getProductName()))
            .setProductImage(nullSafe(dto.getProductImage()))
            .setProductPrice(bigDecimalToString(dto.getPrice()))
            .setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0)
            .setSubtotal(bigDecimalToString(dto.getTotal()))
            .setInStock(dto.isInStock())
            .setAvailableStock(dto.getAvailableStock() != null ? dto.getAvailableStock() : 0)
            .build();
    }

    public static CartResponse toCartResponse(CartDTO dto, String message) {
        return CartResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setCart(toProto(dto))
            .build();
    }

    // ==================== Order ====================

    public static Order toProto(OrderDTO dto) {
        if (dto == null) return Order.getDefaultInstance();

        Order.Builder builder = Order.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setOrderNumber(nullSafe(dto.getOrderNumber()))
            .setUserId(dto.getUserId() != null ? dto.getUserId() : 0)
            .setUserName(nullSafe(dto.getUserName()))
            .setUserEmail(nullSafe(dto.getUserEmail()))
            .setStatus(nullSafe(dto.getStatus()))
            .setSubtotal(bigDecimalToString(dto.getSubtotal()))
            .setShippingCost(bigDecimalToString(dto.getShippingCost()))
            .setTaxAmount(bigDecimalToString(dto.getTaxAmount()))
            .setDiscountAmount(bigDecimalToString(dto.getDiscountAmount()))
            .setTotalAmount(bigDecimalToString(dto.getTotalAmount()))
            .setCouponCode(nullSafe(dto.getCouponCode()))
            .setPaymentMethod(nullSafe(dto.getPaymentMethod()))
            .setPaymentStatus(nullSafe(dto.getPaymentStatus()))
            .setPaymentIntentId(nullSafe(dto.getPaymentIntentId()))
            .setShippingName(nullSafe(dto.getShippingName()))
            .setShippingAddress(nullSafe(dto.getShippingAddress()))
            .setShippingCity(nullSafe(dto.getShippingCity()))
            .setShippingState(nullSafe(dto.getShippingState()))
            .setShippingPostalCode(nullSafe(dto.getShippingPostalCode()))
            .setShippingCountry(nullSafe(dto.getShippingCountry()))
            .setShippingPhone(nullSafe(dto.getShippingPhone()))
            .setTrackingNumber(nullSafe(dto.getTrackingNumber()))
            .setCancellationReason(nullSafe(dto.getCancellationReason()))
            .setCustomerNotes(nullSafe(dto.getCustomerNotes()))
            .setTotalItemCount(dto.getTotalItemCount());

        if (dto.getItems() != null) {
            dto.getItems().forEach(item -> builder.addItems(toProto(item)));
        }

        if (dto.getPaidAt() != null) {
            builder.setPaidAt(toProtoTimestamp(dto.getPaidAt()));
        }
        if (dto.getShippedAt() != null) {
            builder.setShippedAt(toProtoTimestamp(dto.getShippedAt()));
        }
        if (dto.getDeliveredAt() != null) {
            builder.setDeliveredAt(toProtoTimestamp(dto.getDeliveredAt()));
        }
        if (dto.getCancelledAt() != null) {
            builder.setCancelledAt(toProtoTimestamp(dto.getCancelledAt()));
        }
        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    public static OrderItem toProto(OrderItemDTO dto) {
        if (dto == null) return OrderItem.getDefaultInstance();

        return OrderItem.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setProductId(dto.getProductId() != null ? dto.getProductId() : 0)
            .setProductName(nullSafe(dto.getProductName()))
            .setProductImage(nullSafe(dto.getProductImage()))
            .setSku(nullSafe(dto.getProductSku()))
            .setUnitPrice(bigDecimalToString(dto.getUnitPrice()))
            .setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0)
            .setSubtotal(bigDecimalToString(dto.getTotal()))
            .build();
    }

    public static OrderResponse toOrderResponse(OrderDTO dto, String message) {
        return OrderResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setOrder(toProto(dto))
            .build();
    }

    // ==================== User ====================

    public static UserInfo toProto(UserDTO dto) {
        if (dto == null) return UserInfo.getDefaultInstance();

        UserInfo.Builder builder = UserInfo.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setFirstName(nullSafe(dto.getFirstName()))
            .setLastName(nullSafe(dto.getLastName()))
            .setEmail(nullSafe(dto.getEmail()))
            .setPhone(nullSafe(dto.getPhone()))
            .setRole(nullSafe(dto.getRole()))
            .setEnabled(dto.isEnabled())
            .setEmailVerified(dto.isEmailVerified());

        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    // Profile UserInfo conversion
    public static com.wishkart.grpc.profile.UserInfo toProfileUserInfo(UserDTO dto) {
        if (dto == null) return com.wishkart.grpc.profile.UserInfo.getDefaultInstance();

        com.wishkart.grpc.profile.UserInfo.Builder builder = com.wishkart.grpc.profile.UserInfo.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setFirstName(nullSafe(dto.getFirstName()))
            .setLastName(nullSafe(dto.getLastName()))
            .setEmail(nullSafe(dto.getEmail()))
            .setPhone(nullSafe(dto.getPhone()))
            .setRole(nullSafe(dto.getRole()))
            .setEnabled(dto.isEnabled())
            .setEmailVerified(dto.isEmailVerified());

        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    // Admin UserInfo conversion
    public static com.wishkart.grpc.admin.UserInfo toAdminUserInfo(UserDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.UserInfo.getDefaultInstance();

        com.wishkart.grpc.admin.UserInfo.Builder builder = com.wishkart.grpc.admin.UserInfo.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setFirstName(nullSafe(dto.getFirstName()))
            .setLastName(nullSafe(dto.getLastName()))
            .setEmail(nullSafe(dto.getEmail()))
            .setPhone(nullSafe(dto.getPhone()))
            .setRole(nullSafe(dto.getRole()))
            .setEnabled(dto.isEnabled())
            .setEmailVerified(dto.isEmailVerified());

        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    public static AuthResponse toAuthResponse(com.wishkart.dto.AuthResponse dto) {
        if (dto == null) return AuthResponse.getDefaultInstance();

        return AuthResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Authentication successful")
            .setAccessToken(nullSafe(dto.getAccessToken()))
            .setRefreshToken(nullSafe(dto.getRefreshToken()))
            .setTokenType(nullSafe(dto.getTokenType()))
            .setExpiresIn(dto.getExpiresIn() != null ? dto.getExpiresIn() : 0)
            .setUser(toProto(dto.getUser()))
            .build();
    }

    // ==================== Review ====================

    public static Review toProto(ReviewDTO dto) {
        if (dto == null) return Review.getDefaultInstance();

        Review.Builder builder = Review.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setProductId(dto.getProductId() != null ? dto.getProductId() : 0)
            .setProductName(nullSafe(dto.getProductName()))
            .setUserId(dto.getUserId() != null ? dto.getUserId() : 0)
            .setUserName(nullSafe(dto.getUserName()))
            .setRating(dto.getRating() != null ? dto.getRating() : 0)
            .setTitle(nullSafe(dto.getTitle()))
            .setComment(nullSafe(dto.getComment()))
            .setVerified(dto.isVerified())
            .setApproved(dto.isApproved())
            .setHelpfulCount(dto.getHelpfulCount() != null ? dto.getHelpfulCount() : 0);

        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    // Admin Review conversion
    public static com.wishkart.grpc.admin.Review toAdminReview(ReviewDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.Review.getDefaultInstance();

        com.wishkart.grpc.admin.Review.Builder builder = com.wishkart.grpc.admin.Review.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setProductId(dto.getProductId() != null ? dto.getProductId() : 0)
            .setProductName(nullSafe(dto.getProductName()))
            .setUserId(dto.getUserId() != null ? dto.getUserId() : 0)
            .setUserName(nullSafe(dto.getUserName()))
            .setRating(dto.getRating() != null ? dto.getRating() : 0)
            .setTitle(nullSafe(dto.getTitle()))
            .setComment(nullSafe(dto.getComment()))
            .setVerified(dto.isVerified())
            .setApproved(dto.isApproved())
            .setHelpfulCount(dto.getHelpfulCount() != null ? dto.getHelpfulCount() : 0);

        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    public static ReviewStats toProto(com.wishkart.service.ReviewService.ReviewStatsDTO dto) {
        if (dto == null) return ReviewStats.getDefaultInstance();

        return ReviewStats.newBuilder()
            .setAverageRating(dto.getAverageRating() != null ? dto.getAverageRating() : 0.0)
            .setTotalReviews(dto.getTotalReviews() != null ? dto.getTotalReviews().intValue() : 0)
            .setFiveStarCount(dto.getFiveStarCount())
            .setFourStarCount(dto.getFourStarCount())
            .setThreeStarCount(dto.getThreeStarCount())
            .setTwoStarCount(dto.getTwoStarCount())
            .setOneStarCount(dto.getOneStarCount())
            .build();
    }

    // ==================== Wishlist ====================

    public static Wishlist toProto(WishlistDTO dto) {
        if (dto == null) return Wishlist.getDefaultInstance();

        Wishlist.Builder builder = Wishlist.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setUserId(dto.getUserId() != null ? dto.getUserId() : 0)
            .setItemCount(dto.getItemCount());

        if (dto.getProducts() != null) {
            dto.getProducts().forEach(p -> builder.addProducts(toWishlistProduct(p)));
        }

        return builder.build();
    }

    public static WishlistProduct toWishlistProduct(ProductDTO dto) {
        if (dto == null) return WishlistProduct.getDefaultInstance();

        return WishlistProduct.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setName(nullSafe(dto.getName()))
            .setSlug(nullSafe(dto.getSlug()))
            .setPrice(bigDecimalToString(dto.getPrice()))
            .setPrimaryImage(nullSafe(dto.getPrimaryImage()))
            .setInStock(dto.isInStock())
            .setAverageRating(dto.getAverageRating())
            .build();
    }

    // ==================== Pagination ====================

    public static com.wishkart.grpc.product.PaginationInfo toProductPaginationInfo(Page<?> page) {
        return com.wishkart.grpc.product.PaginationInfo.newBuilder()
            .setPage(page.getNumber())
            .setSize(page.getSize())
            .setTotalElements(page.getTotalElements())
            .setTotalPages(page.getTotalPages())
            .setFirst(page.isFirst())
            .setLast(page.isLast())
            .setHasNext(page.hasNext())
            .setHasPrevious(page.hasPrevious())
            .build();
    }

    public static com.wishkart.grpc.order.PaginationInfo toOrderPaginationInfo(Page<?> page) {
        return com.wishkart.grpc.order.PaginationInfo.newBuilder()
            .setPage(page.getNumber())
            .setSize(page.getSize())
            .setTotalElements(page.getTotalElements())
            .setTotalPages(page.getTotalPages())
            .setFirst(page.isFirst())
            .setLast(page.isLast())
            .setHasNext(page.hasNext())
            .setHasPrevious(page.hasPrevious())
            .build();
    }

    public static com.wishkart.grpc.review.PaginationInfo toReviewPaginationInfo(Page<?> page) {
        return com.wishkart.grpc.review.PaginationInfo.newBuilder()
            .setPage(page.getNumber())
            .setSize(page.getSize())
            .setTotalElements(page.getTotalElements())
            .setTotalPages(page.getTotalPages())
            .setFirst(page.isFirst())
            .setLast(page.isLast())
            .setHasNext(page.hasNext())
            .setHasPrevious(page.hasPrevious())
            .build();
    }

    public static com.wishkart.grpc.review.PaginationInfo toReviewPaginationInfo(PagedResponse<?> response) {
        return com.wishkart.grpc.review.PaginationInfo.newBuilder()
            .setPage(response.getPage())
            .setSize(response.getSize())
            .setTotalElements(response.getTotalElements())
            .setTotalPages(response.getTotalPages())
            .setFirst(response.getPage() == 0)
            .setLast(response.getPage() >= response.getTotalPages() - 1)
            .setHasNext(response.getPage() < response.getTotalPages() - 1)
            .setHasPrevious(response.getPage() > 0)
            .build();
    }

    public static com.wishkart.grpc.admin.PaginationInfo toAdminPaginationInfo(Page<?> page) {
        return com.wishkart.grpc.admin.PaginationInfo.newBuilder()
            .setPage(page.getNumber())
            .setSize(page.getSize())
            .setTotalElements(page.getTotalElements())
            .setTotalPages(page.getTotalPages())
            .setFirst(page.isFirst())
            .setLast(page.isLast())
            .setHasNext(page.hasNext())
            .setHasPrevious(page.hasPrevious())
            .build();
    }

    public static com.wishkart.grpc.admin.PaginationInfo toAdminPaginationInfo(PagedResponse<?> response) {
        return com.wishkart.grpc.admin.PaginationInfo.newBuilder()
            .setPage(response.getPage())
            .setSize(response.getSize())
            .setTotalElements(response.getTotalElements())
            .setTotalPages(response.getTotalPages())
            .setFirst(response.getPage() == 0)
            .setLast(response.getPage() >= response.getTotalPages() - 1)
            .setHasNext(response.getPage() < response.getTotalPages() - 1)
            .setHasPrevious(response.getPage() > 0)
            .build();
    }

    // ==================== Common ====================

    public static com.wishkart.grpc.auth.ApiResponse toAuthApiResponse(boolean success, String message) {
        return com.wishkart.grpc.auth.ApiResponse.newBuilder()
            .setSuccess(success)
            .setMessage(nullSafe(message))
            .setTimestamp(toProtoTimestamp(LocalDateTime.now()))
            .build();
    }

    public static com.wishkart.grpc.profile.ApiResponse toProfileApiResponse(boolean success, String message) {
        return com.wishkart.grpc.profile.ApiResponse.newBuilder()
            .setSuccess(success)
            .setMessage(nullSafe(message))
            .setTimestamp(toProtoTimestamp(LocalDateTime.now()))
            .build();
    }

    public static com.wishkart.grpc.review.ApiResponse toReviewApiResponse(boolean success, String message) {
        return com.wishkart.grpc.review.ApiResponse.newBuilder()
            .setSuccess(success)
            .setMessage(nullSafe(message))
            .setTimestamp(toProtoTimestamp(LocalDateTime.now()))
            .build();
    }

    public static com.wishkart.grpc.wishlist.ApiResponse toWishlistApiResponse(boolean success, String message) {
        return com.wishkart.grpc.wishlist.ApiResponse.newBuilder()
            .setSuccess(success)
            .setMessage(nullSafe(message))
            .setTimestamp(toProtoTimestamp(LocalDateTime.now()))
            .build();
    }

    public static com.wishkart.grpc.admin.ApiResponse toAdminApiResponse(boolean success, String message) {
        return com.wishkart.grpc.admin.ApiResponse.newBuilder()
            .setSuccess(success)
            .setMessage(nullSafe(message))
            .setTimestamp(toProtoTimestamp(LocalDateTime.now()))
            .build();
    }

    // ==================== Admin-specific types ====================

    public static com.wishkart.grpc.admin.Product toAdminProduct(ProductDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.Product.getDefaultInstance();

        com.wishkart.grpc.admin.Product.Builder builder = com.wishkart.grpc.admin.Product.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setName(nullSafe(dto.getName()))
            .setSku(nullSafe(dto.getSku()))
            .setSlug(nullSafe(dto.getSlug()))
            .setDescription(nullSafe(dto.getDescription()))
            .setShortDescription(nullSafe(dto.getShortDescription()))
            .setPrice(bigDecimalToString(dto.getPrice()))
            .setCompareAtPrice(bigDecimalToString(dto.getCompareAtPrice()))
            .setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
            .setInStock(dto.isInStock())
            .setLowStock(dto.isLowStock())
            .setActive(dto.isActive())
            .setFeatured(dto.isFeatured())
            .setWeight(bigDecimalToString(dto.getWeight()))
            .setWeightUnit(nullSafe(dto.getWeightUnit()))
            .setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : 0)
            .setCategoryName(nullSafe(dto.getCategoryName()))
            .setPrimaryImage(nullSafe(dto.getPrimaryImage()))
            .setAverageRating(dto.getAverageRating())
            .setReviewCount(dto.getReviewCount())
            .setDiscountPercentage(bigDecimalToString(dto.getDiscountPercentage()));

        if (dto.getImages() != null) {
            builder.addAllImages(dto.getImages());
        }

        return builder.build();
    }

    public static com.wishkart.grpc.admin.ProductResponse toAdminProductResponse(ProductDTO dto, String message) {
        return com.wishkart.grpc.admin.ProductResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setProduct(toAdminProduct(dto))
            .build();
    }

    public static com.wishkart.grpc.admin.Category toAdminCategory(CategoryDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.Category.getDefaultInstance();

        com.wishkart.grpc.admin.Category.Builder builder = com.wishkart.grpc.admin.Category.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setName(nullSafe(dto.getName()))
            .setSlug(nullSafe(dto.getSlug()))
            .setDescription(nullSafe(dto.getDescription()))
            .setImageUrl(nullSafe(dto.getImageUrl()))
            .setActive(dto.isActive())
            .setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
            .setParentId(dto.getParentId() != null ? dto.getParentId() : 0)
            .setParentName(nullSafe(dto.getParentName()))
            .setProductCount(dto.getProductCount());

        if (dto.getSubcategories() != null) {
            dto.getSubcategories().forEach(sub -> builder.addSubcategories(toAdminCategory(sub)));
        }

        return builder.build();
    }

    public static com.wishkart.grpc.admin.CategoryResponse toAdminCategoryResponse(CategoryDTO dto, String message) {
        return com.wishkart.grpc.admin.CategoryResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setCategory(toAdminCategory(dto))
            .build();
    }

    public static com.wishkart.grpc.admin.Order toAdminOrder(OrderDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.Order.getDefaultInstance();

        com.wishkart.grpc.admin.Order.Builder builder = com.wishkart.grpc.admin.Order.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setOrderNumber(nullSafe(dto.getOrderNumber()))
            .setUserId(dto.getUserId() != null ? dto.getUserId() : 0)
            .setUserName(nullSafe(dto.getUserName()))
            .setUserEmail(nullSafe(dto.getUserEmail()))
            .setStatus(nullSafe(dto.getStatus()))
            .setSubtotal(bigDecimalToString(dto.getSubtotal()))
            .setShippingCost(bigDecimalToString(dto.getShippingCost()))
            .setTaxAmount(bigDecimalToString(dto.getTaxAmount()))
            .setDiscountAmount(bigDecimalToString(dto.getDiscountAmount()))
            .setTotalAmount(bigDecimalToString(dto.getTotalAmount()))
            .setCouponCode(nullSafe(dto.getCouponCode()))
            .setPaymentMethod(nullSafe(dto.getPaymentMethod()))
            .setPaymentStatus(nullSafe(dto.getPaymentStatus()))
            .setPaymentIntentId(nullSafe(dto.getPaymentIntentId()))
            .setShippingName(nullSafe(dto.getShippingName()))
            .setShippingAddress(nullSafe(dto.getShippingAddress()))
            .setShippingCity(nullSafe(dto.getShippingCity()))
            .setShippingState(nullSafe(dto.getShippingState()))
            .setShippingPostalCode(nullSafe(dto.getShippingPostalCode()))
            .setShippingCountry(nullSafe(dto.getShippingCountry()))
            .setShippingPhone(nullSafe(dto.getShippingPhone()))
            .setTrackingNumber(nullSafe(dto.getTrackingNumber()))
            .setCancellationReason(nullSafe(dto.getCancellationReason()))
            .setCustomerNotes(nullSafe(dto.getCustomerNotes()))
            .setTotalItemCount(dto.getTotalItemCount());

        if (dto.getItems() != null) {
            dto.getItems().forEach(item -> builder.addItems(toAdminOrderItem(item)));
        }

        if (dto.getPaidAt() != null) {
            builder.setPaidAt(toProtoTimestamp(dto.getPaidAt()));
        }
        if (dto.getShippedAt() != null) {
            builder.setShippedAt(toProtoTimestamp(dto.getShippedAt()));
        }
        if (dto.getDeliveredAt() != null) {
            builder.setDeliveredAt(toProtoTimestamp(dto.getDeliveredAt()));
        }
        if (dto.getCancelledAt() != null) {
            builder.setCancelledAt(toProtoTimestamp(dto.getCancelledAt()));
        }
        if (dto.getCreatedAt() != null) {
            builder.setCreatedAt(toProtoTimestamp(dto.getCreatedAt()));
        }

        return builder.build();
    }

    public static com.wishkart.grpc.admin.OrderItem toAdminOrderItem(OrderItemDTO dto) {
        if (dto == null) return com.wishkart.grpc.admin.OrderItem.getDefaultInstance();

        return com.wishkart.grpc.admin.OrderItem.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0)
            .setProductId(dto.getProductId() != null ? dto.getProductId() : 0)
            .setProductName(nullSafe(dto.getProductName()))
            .setProductImage(nullSafe(dto.getProductImage()))
            .setSku(nullSafe(dto.getProductSku()))
            .setUnitPrice(bigDecimalToString(dto.getUnitPrice()))
            .setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0)
            .setSubtotal(bigDecimalToString(dto.getTotal()))
            .build();
    }

    public static com.wishkart.grpc.admin.OrderResponse toAdminOrderResponse(OrderDTO dto, String message) {
        return com.wishkart.grpc.admin.OrderResponse.newBuilder()
            .setSuccess(true)
            .setMessage(nullSafe(message))
            .setOrder(toAdminOrder(dto))
            .build();
    }

    // ==================== Timestamp ====================

    public static Timestamp toProtoTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    public static LocalDateTime fromProtoTimestamp(Timestamp timestamp) {
        if (timestamp == null || timestamp.equals(Timestamp.getDefaultInstance())) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(
            timestamp.getSeconds(),
            timestamp.getNanos(),
            ZoneOffset.UTC);
    }

    // ==================== Helper Methods ====================

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    private static String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toString() : "0";
    }

    public static BigDecimal stringToBigDecimal(String value) {
        try {
            return value != null && !value.isEmpty() ? new BigDecimal(value) : BigDecimal.ZERO;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
