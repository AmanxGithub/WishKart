package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.CreateReviewRequest;
import com.wishkart.dto.PagedResponse;
import com.wishkart.dto.ReviewDTO;
import com.wishkart.service.ReviewService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for product reviews.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review operations")
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getProductReviews(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {
        PagedResponse<ReviewDTO> reviews = reviewService.getProductReviews(productId, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/products/{productId}/reviews/stats")
    @Operation(summary = "Get review statistics for a product")
    public ResponseEntity<ApiResponse<ReviewService.ReviewStatsDTO>> getProductReviewStats(
            @PathVariable("productId") Long productId) {
        ReviewService.ReviewStatsDTO stats = reviewService.getProductReviewStats(productId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== AUTHENTICATED ENDPOINTS ====================

    @PostMapping("/reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a review")
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        ReviewDTO review = reviewService.createReview(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", review));
    }

    @GetMapping("/reviews/my-reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user's reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getMyReviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        PagedResponse<ReviewDTO> reviews = reviewService.getUserReviews(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PutMapping("/reviews/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a review")
    public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody CreateReviewRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        ReviewDTO review = reviewService.updateReview(reviewId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable("reviewId") Long reviewId) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    @PostMapping("/reviews/{reviewId}/helpful")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a review as helpful")
    public ResponseEntity<ApiResponse<ReviewDTO>> markAsHelpful(@PathVariable("reviewId") Long reviewId) {
        ReviewDTO review = reviewService.markAsHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/reviews/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get pending reviews for moderation")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getPendingReviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResponse<ReviewDTO> reviews = reviewService.getPendingReviews(page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PostMapping("/admin/reviews/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Approve or reject a review")
    public ResponseEntity<ApiResponse<ReviewDTO>> moderateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("approved") boolean approved) {
        ReviewDTO review = reviewService.moderateReview(reviewId, approved);
        String message = approved ? "Review approved" : "Review rejected";
        return ResponseEntity.ok(ApiResponse.success(message, review));
    }

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin delete any review")
    public ResponseEntity<ApiResponse<Void>> adminDeleteReview(@PathVariable("reviewId") Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted by admin"));
    }
}
