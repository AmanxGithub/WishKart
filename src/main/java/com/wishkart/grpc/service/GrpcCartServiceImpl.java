package com.wishkart.grpc.service;

import com.wishkart.dto.CartDTO;
import com.wishkart.grpc.cart.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.CartService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC service implementation for Cart operations.
 * All methods require authentication.
 * Delegates to existing CartService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcCartServiceImpl extends CartServiceGrpc.CartServiceImplBase {

    private final CartService cartService;

    @Override
    public void getCart(Empty request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetCart request for user: {}", userId);

            CartDTO cart = cartService.getCart(userId);

            CartResponse response = ProtoConverter.toCartResponse(cart, "Cart retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void addToCart(AddToCartRequest request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC AddToCart request for user: {}, product: {}, qty: {}",
                userId, request.getProductId(), request.getQuantity());

            CartDTO cart = cartService.addToCart(userId, request.getProductId(), request.getQuantity());

            CartResponse response = ProtoConverter.toCartResponse(cart, "Item added to cart");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateCartItem(UpdateCartItemRequest request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC UpdateCartItem request for user: {}, product: {}, qty: {}",
                userId, request.getProductId(), request.getQuantity());

            CartDTO cart = cartService.updateCartItemQuantity(userId, request.getProductId(), request.getQuantity());

            CartResponse response = ProtoConverter.toCartResponse(cart, "Cart updated");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void removeFromCart(RemoveFromCartRequest request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC RemoveFromCart request for user: {}, product: {}",
                userId, request.getProductId());

            CartDTO cart = cartService.removeFromCart(userId, request.getProductId());

            CartResponse response = ProtoConverter.toCartResponse(cart, "Item removed from cart");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void clearCart(Empty request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ClearCart request for user: {}", userId);

            CartDTO cart = cartService.clearCart(userId);

            CartResponse response = ProtoConverter.toCartResponse(cart, "Cart cleared");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getCartItemCount(Empty request, StreamObserver<CartCountResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetCartItemCount request for user: {}", userId);

            int count = cartService.getCartItemCount(userId);

            CartCountResponse response = CartCountResponse.newBuilder()
                .setSuccess(true)
                .setCount(count)
                .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void applyCoupon(ApplyCouponRequest request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ApplyCoupon request for user: {}, code: {}",
                userId, request.getCouponCode());

            CartDTO cart = cartService.applyCoupon(userId, request.getCouponCode());

            CartResponse response = ProtoConverter.toCartResponse(cart, "Coupon applied successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void removeCoupon(Empty request, StreamObserver<CartResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC RemoveCoupon request for user: {}", userId);

            CartDTO cart = cartService.removeCoupon(userId);

            CartResponse response = ProtoConverter.toCartResponse(cart, "Coupon removed");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
