package com.wishkart.grpc.service;

import com.wishkart.dto.CheckoutRequest;
import com.wishkart.dto.OrderDTO;
import com.wishkart.grpc.order.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.OrderService;
import com.wishkart.service.PaymentService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * gRPC service implementation for Order operations.
 * Delegates to existing OrderService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcOrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Override
    public void checkout(com.wishkart.grpc.order.CheckoutRequest request,
                         StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC Checkout request for user: {}", userId);

            // Convert gRPC request to DTO
            CheckoutRequest checkoutRequest = new CheckoutRequest();
            checkoutRequest.setShippingName(request.getShippingName());
            checkoutRequest.setShippingAddressLine1(request.getShippingAddressLine1());
            checkoutRequest.setShippingAddressLine2(request.getShippingAddressLine2());
            checkoutRequest.setShippingCity(request.getShippingCity());
            checkoutRequest.setShippingState(request.getShippingState());
            checkoutRequest.setShippingPostalCode(request.getShippingPostalCode());
            checkoutRequest.setShippingCountry(request.getShippingCountry());
            checkoutRequest.setShippingPhone(request.getShippingPhone());
            checkoutRequest.setPaymentMethod(request.getPaymentMethod());
            checkoutRequest.setCouponCode(request.getCouponCode());
            checkoutRequest.setCustomerNotes(request.getCustomerNotes());
            checkoutRequest.setBillingSameAsShipping(request.getBillingSameAsShipping());

            // Call service
            OrderDTO order = orderService.createOrder(userId, checkoutRequest);

            OrderResponse response = ProtoConverter.toOrderResponse(order, "Order created successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getMyOrders(PaginationRequest request,
                            StreamObserver<OrderListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetMyOrders request for user: {}", userId);

            PageRequest pageRequest = buildPageRequest(request);
            Page<OrderDTO> orders = orderService.getOrdersByUser(userId, pageRequest);

            OrderListResponse.Builder builder = OrderListResponse.newBuilder()
                .setSuccess(true)
                .setPagination(ProtoConverter.toOrderPaginationInfo(orders));

            orders.getContent().forEach(o -> builder.addOrders(ProtoConverter.toProto(o)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getOrderById(OrderIdRequest request,
                             StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetOrderById request for order: {}", request.getId());

            OrderDTO order = orderService.getOrderById(request.getId());

            // Verify user owns this order
            if (!order.getUserId().equals(userId)) {
                GrpcSecurityUtil.requireAdmin();
            }

            OrderResponse response = ProtoConverter.toOrderResponse(order, "Order retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getOrderByNumber(OrderNumberRequest request,
                                 StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetOrderByNumber request for order: {}", request.getOrderNumber());

            OrderDTO order = orderService.getOrderByNumber(request.getOrderNumber());

            // Verify user owns this order
            if (!order.getUserId().equals(userId)) {
                GrpcSecurityUtil.requireAdmin();
            }

            OrderResponse response = ProtoConverter.toOrderResponse(order, "Order retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void cancelOrder(CancelOrderRequest request,
                            StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC CancelOrder request for order: {}", request.getOrderId());

            // Verify user owns this order
            OrderDTO existingOrder = orderService.getOrderById(request.getOrderId());
            if (!existingOrder.getUserId().equals(userId)) {
                GrpcSecurityUtil.requireAdmin();
            }

            OrderDTO order = orderService.cancelOrder(request.getOrderId(), request.getReason());

            OrderResponse response = ProtoConverter.toOrderResponse(order, "Order cancelled successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void createPaymentIntent(PaymentIntentRequest request,
                                    StreamObserver<PaymentIntentResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC CreatePaymentIntent request for order: {}", request.getOrderId());

            Map<String, Object> paymentIntent = paymentService.createPaymentIntent(request.getOrderId());

            PaymentIntentResponse response = PaymentIntentResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Payment intent created")
                .setClientSecret(String.valueOf(paymentIntent.getOrDefault("clientSecret", "")))
                .setPaymentIntentId(String.valueOf(paymentIntent.getOrDefault("paymentIntentId", "")))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void confirmPayment(ConfirmPaymentRequest request,
                               StreamObserver<OrderResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ConfirmPayment request for paymentIntent: {}", request.getPaymentIntentId());

            OrderDTO order = paymentService.confirmPayment(request.getPaymentIntentId());

            OrderResponse response = ProtoConverter.toOrderResponse(order, "Payment confirmed successfully");
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
