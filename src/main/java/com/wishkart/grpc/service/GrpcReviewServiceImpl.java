package com.wishkart.grpc.service;

import com.wishkart.dto.CreateReviewRequest;
import com.wishkart.dto.PagedResponse;
import com.wishkart.dto.ReviewDTO;
import com.wishkart.service.ReviewService.ReviewStatsDTO;
import com.wishkart.grpc.review.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.ReviewService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC service implementation for Review operations.
 * Some methods are public, others require authentication.
 * Delegates to existing ReviewService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcReviewServiceImpl extends ReviewServiceGrpc.ReviewServiceImplBase {

    private final ReviewService reviewService;

    // Public endpoint
    @Override
    public void getProductReviews(GetProductReviewsRequest request,
                                  StreamObserver<ReviewListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductReviews request for product: {}", request.getProductId());

            int page = request.getPage() >= 0 ? request.getPage() : 0;
            int size = request.getSize() > 0 ? request.getSize() : 10;
            String sort = request.getSort().isEmpty() ? "newest" : request.getSort();

            PagedResponse<ReviewDTO> reviews = reviewService.getProductReviews(
                request.getProductId(), page, size, sort);

            ReviewListResponse.Builder builder = ReviewListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toReviewPaginationInfo(reviews));

            reviews.getContent().forEach(r -> builder.addReviews(ProtoConverter.toProto(r)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Public endpoint
    @Override
    public void getProductReviewStats(ProductIdRequest request,
                                      StreamObserver<ReviewStatsResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductReviewStats request for product: {}", request.getProductId());

            ReviewStatsDTO stats = reviewService.getProductReviewStats(request.getProductId());

            ReviewStatsResponse response = ReviewStatsResponse.newBuilder()
                .setSuccess(true)
                .setStats(ProtoConverter.toProto(stats))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Authenticated endpoint
    @Override
    public void createReview(com.wishkart.grpc.review.CreateReviewRequest request,
                             StreamObserver<ReviewResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC CreateReview request for user: {}, product: {}",
                userId, request.getProductId());

            // Convert gRPC request to DTO
            CreateReviewRequest createRequest = new CreateReviewRequest();
            createRequest.setProductId(request.getProductId());
            createRequest.setRating(request.getRating());
            createRequest.setTitle(request.getTitle());
            createRequest.setComment(request.getComment());

            ReviewDTO review = reviewService.createReview(userId, createRequest);

            ReviewResponse response = ReviewResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Review created successfully")
                .setReview(ProtoConverter.toProto(review))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Authenticated endpoint
    @Override
    public void getMyReviews(PaginationRequest request,
                             StreamObserver<ReviewListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetMyReviews request for user: {}", userId);

            int page = request != null && request.getPage() >= 0 ? request.getPage() : 0;
            int size = request != null && request.getSize() > 0 ? request.getSize() : 10;

            PagedResponse<ReviewDTO> reviews = reviewService.getUserReviews(userId, page, size);

            ReviewListResponse.Builder builder = ReviewListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toReviewPaginationInfo(reviews));

            reviews.getContent().forEach(r -> builder.addReviews(ProtoConverter.toProto(r)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Authenticated endpoint
    @Override
    public void updateReview(UpdateReviewRequest request,
                             StreamObserver<ReviewResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC UpdateReview request for user: {}, review: {}",
                userId, request.getReviewId());

            // Convert gRPC request to DTO
            CreateReviewRequest updateRequest = new CreateReviewRequest();
            updateRequest.setRating(request.getRating());
            updateRequest.setTitle(request.getTitle());
            updateRequest.setComment(request.getComment());

            ReviewDTO review = reviewService.updateReview(userId, request.getReviewId(), updateRequest);

            ReviewResponse response = ReviewResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Review updated successfully")
                .setReview(ProtoConverter.toProto(review))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Authenticated endpoint
    @Override
    public void deleteReview(ReviewIdRequest request,
                             StreamObserver<com.wishkart.grpc.review.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC DeleteReview request for user: {}, review: {}",
                userId, request.getReviewId());

            reviewService.deleteReview(userId, request.getReviewId());

            com.wishkart.grpc.review.ApiResponse response = ProtoConverter.toReviewApiResponse(true, "Review deleted successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Authenticated endpoint
    @Override
    public void markAsHelpful(ReviewIdRequest request,
                              StreamObserver<ReviewResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC MarkAsHelpful request for review: {}", request.getReviewId());

            ReviewDTO review = reviewService.markAsHelpful(request.getReviewId());

            ReviewResponse response = ReviewResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Marked as helpful")
                .setReview(ProtoConverter.toProto(review))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
