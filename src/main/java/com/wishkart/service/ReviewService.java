package com.wishkart.service;

import com.wishkart.dto.CreateReviewRequest;
import com.wishkart.dto.PagedResponse;
import com.wishkart.dto.ReviewDTO;
import com.wishkart.entity.Product;
import com.wishkart.entity.Review;
import com.wishkart.entity.User;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.OrderRepository;
import com.wishkart.repository.ProductRepository;
import com.wishkart.repository.ReviewRepository;
import com.wishkart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing product reviews.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * Creates a new review for a product.
     */
    public ReviewDTO createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(product.getId(), userId)) {
            throw new BadRequestException("You have already reviewed this product");
        }

        // Check if user has purchased this product (for verified reviews)
        boolean verified = orderRepository.hasUserPurchasedProduct(userId, product.getId());

        Review review = Review.builder()
            .product(product)
            .user(user)
            .rating(request.getRating())
            .title(request.getTitle())
            .comment(request.getComment())
            .verified(verified)
            .approved(true) // Auto-approve for now, could add moderation
            .helpfulCount(0)
            .build();

        review = reviewRepository.save(review);

        // Update product average rating
        updateProductRating(product.getId());

        log.info("Review created for product {} by user {}", product.getId(), userId);
        return mapToDTO(review);
    }

    /**
     * Gets all approved reviews for a product.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewDTO> getProductReviews(Long productId, int page, int size, String sortBy) {
        Sort sort = switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "highest" -> Sort.by(Sort.Direction.DESC, "rating");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "rating");
            case "helpful" -> Sort.by(Sort.Direction.DESC, "helpfulCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Page<Review> reviewPage = reviewRepository.findByProductIdAndApprovedTrue(
            productId, PageRequest.of(page, size, sort));

        List<ReviewDTO> reviews = reviewPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PagedResponse.of(reviewPage, reviews);
    }

    /**
     * Gets review statistics for a product.
     */
    @Transactional(readOnly = true)
    public ReviewStatsDTO getProductReviewStats(Long productId) {
        Long totalReviews = reviewRepository.countByProductIdAndApprovedTrue(productId);
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);

        // Get rating distribution
        List<Object[]> distribution = reviewRepository.getRatingDistribution(productId);

        int[] ratingCounts = new int[5];
        for (Object[] row : distribution) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            ratingCounts[rating - 1] = (int) count;
        }

        return ReviewStatsDTO.builder()
            .totalReviews(totalReviews)
            .averageRating(averageRating != null ? averageRating : 0.0)
            .fiveStarCount(ratingCounts[4])
            .fourStarCount(ratingCounts[3])
            .threeStarCount(ratingCounts[2])
            .twoStarCount(ratingCounts[1])
            .oneStarCount(ratingCounts[0])
            .build();
    }

    /**
     * Gets all reviews by a user.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewDTO> getUserReviews(Long userId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByUserId(
            userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<ReviewDTO> reviews = reviewPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PagedResponse.of(reviewPage, reviews);
    }

    /**
     * Updates a review.
     */
    public ReviewDTO updateReview(Long reviewId, Long userId, CreateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        // Update product average rating
        updateProductRating(review.getProduct().getId());

        return mapToDTO(review);
    }

    /**
     * Deletes a review.
     */
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Update product average rating
        updateProductRating(productId);

        log.info("Review {} deleted by user {}", reviewId, userId);
    }

    /**
     * Admin: Delete any review.
     */
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        updateProductRating(productId);

        log.info("Review {} deleted by admin", reviewId);
    }

    /**
     * Marks a review as helpful.
     */
    public ReviewDTO markAsHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.incrementHelpfulCount();
        review = reviewRepository.save(review);

        return mapToDTO(review);
    }

    /**
     * Admin: Approve or reject a review.
     */
    public ReviewDTO moderateReview(Long reviewId, boolean approved) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setApproved(approved);
        review = reviewRepository.save(review);

        if (approved) {
            updateProductRating(review.getProduct().getId());
        }

        log.info("Review {} {}", reviewId, approved ? "approved" : "rejected");
        return mapToDTO(review);
    }

    /**
     * Admin: Get pending reviews for moderation.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewDTO> getPendingReviews(int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByApprovedFalse(
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));

        List<ReviewDTO> reviews = reviewPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PagedResponse.of(reviewPage, reviews);
    }

    /**
     * Updates the average rating on the product.
     */
    private void updateProductRating(Long productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.countByProductIdAndApprovedTrue(productId);

        productRepository.findById(productId).ifPresent(product -> {
            product.setAverageRating(avgRating != null ? avgRating : 0.0);
            product.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);
            productRepository.save(product);
        });
    }

    private ReviewDTO mapToDTO(Review review) {
        return ReviewDTO.builder()
            .id(review.getId())
            .productId(review.getProduct().getId())
            .productName(review.getProduct().getName())
            .userId(review.getUser().getId())
            .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName().charAt(0) + ".")
            .rating(review.getRating())
            .title(review.getTitle())
            .comment(review.getComment())
            .verified(review.isVerified())
            .approved(review.isApproved())
            .helpfulCount(review.getHelpfulCount())
            .createdAt(review.getCreatedAt())
            .build();
    }

    // Inner DTO for review statistics
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReviewStatsDTO {
        private Long totalReviews;
        private Double averageRating;
        private int fiveStarCount;
        private int fourStarCount;
        private int threeStarCount;
        private int twoStarCount;
        private int oneStarCount;
    }
}
