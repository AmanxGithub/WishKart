package com.wishkart.grpc.service;

import com.wishkart.dto.WishlistDTO;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.grpc.wishlist.*;
import com.wishkart.service.CartService;
import com.wishkart.service.WishlistService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC service implementation for Wishlist operations.
 * All methods require authentication.
 * Delegates to existing WishlistService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcWishlistServiceImpl extends WishlistServiceGrpc.WishlistServiceImplBase {

    private final WishlistService wishlistService;
    private final CartService cartService;

    @Override
    public void getWishlist(Empty request, StreamObserver<WishlistResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetWishlist request for user: {}", userId);

            WishlistDTO wishlist = wishlistService.getWishlist(userId);

            WishlistResponse response = WishlistResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Wishlist retrieved successfully")
                .setWishlist(ProtoConverter.toProto(wishlist))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void addToWishlist(ProductIdRequest request, StreamObserver<WishlistResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC AddToWishlist request for user: {}, product: {}",
                userId, request.getProductId());

            WishlistDTO wishlist = wishlistService.addToWishlist(userId, request.getProductId());

            WishlistResponse response = WishlistResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Product added to wishlist")
                .setWishlist(ProtoConverter.toProto(wishlist))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void removeFromWishlist(ProductIdRequest request, StreamObserver<WishlistResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC RemoveFromWishlist request for user: {}, product: {}",
                userId, request.getProductId());

            WishlistDTO wishlist = wishlistService.removeFromWishlist(userId, request.getProductId());

            WishlistResponse response = WishlistResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Product removed from wishlist")
                .setWishlist(ProtoConverter.toProto(wishlist))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void toggleWishlistItem(ProductIdRequest request, StreamObserver<ToggleWishlistResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ToggleWishlistItem request for user: {}, product: {}",
                userId, request.getProductId());

            WishlistDTO wishlist = wishlistService.toggleWishlistItem(userId, request.getProductId());
            boolean inWishlist = wishlistService.isInWishlist(userId, request.getProductId());

            ToggleWishlistResponse response = ToggleWishlistResponse.newBuilder()
                .setSuccess(true)
                .setMessage(inWishlist ? "Product added to wishlist" : "Product removed from wishlist")
                .setInWishlist(inWishlist)
                .setWishlist(ProtoConverter.toProto(wishlist))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void clearWishlist(Empty request, StreamObserver<com.wishkart.grpc.wishlist.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ClearWishlist request for user: {}", userId);

            wishlistService.clearWishlist(userId);

            com.wishkart.grpc.wishlist.ApiResponse response = ProtoConverter.toWishlistApiResponse(true, "Wishlist cleared");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void isInWishlist(ProductIdRequest request, StreamObserver<WishlistCheckResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC IsInWishlist request for user: {}, product: {}",
                userId, request.getProductId());

            boolean inWishlist = wishlistService.isInWishlist(userId, request.getProductId());

            WishlistCheckResponse response = WishlistCheckResponse.newBuilder()
                .setInWishlist(inWishlist)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getWishlistCount(Empty request, StreamObserver<WishlistCountResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetWishlistCount request for user: {}", userId);

            int count = wishlistService.getWishlistCount(userId);

            WishlistCountResponse response = WishlistCountResponse.newBuilder()
                .setCount(count)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void moveToCart(ProductIdRequest request, StreamObserver<WishlistResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC MoveToCart request for user: {}, product: {}",
                userId, request.getProductId());

            WishlistDTO wishlist = wishlistService.moveToCart(userId, request.getProductId(), cartService);

            WishlistResponse response = WishlistResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Product moved to cart")
                .setWishlist(ProtoConverter.toProto(wishlist))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void moveAllToCart(Empty request, StreamObserver<com.wishkart.grpc.wishlist.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC MoveAllToCart request for user: {}", userId);

            wishlistService.moveAllToCart(userId, cartService);

            com.wishkart.grpc.wishlist.ApiResponse response = ProtoConverter.toWishlistApiResponse(true,
                "All items moved to cart");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
