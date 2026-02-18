package com.wishkart.grpc.service;

import com.wishkart.dto.AuthRequest;
import com.wishkart.dto.AuthResponse;
import com.wishkart.dto.RegisterRequest;
import com.wishkart.grpc.auth.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.AuthService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * gRPC service implementation for Authentication.
 * Delegates to existing AuthService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "grpc.server.port", matchIfMissing = false)
public class GrpcAuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Override
    public void register(com.wishkart.grpc.auth.RegisterRequest request,
                         StreamObserver<com.wishkart.grpc.auth.AuthResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC Register request for email: {}", request.getEmail());

            // Convert gRPC request to DTO
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setFirstName(request.getFirstName());
            registerRequest.setLastName(request.getLastName());
            registerRequest.setEmail(request.getEmail());
            registerRequest.setPassword(request.getPassword());
            registerRequest.setConfirmPassword(request.getConfirmPassword());
            registerRequest.setPhone(request.getPhone());

            // Call service
            AuthResponse authResponse = authService.register(registerRequest);

            // Convert response to proto
            com.wishkart.grpc.auth.AuthResponse response = ProtoConverter.toAuthResponse(authResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void login(LoginRequest request,
                      StreamObserver<com.wishkart.grpc.auth.AuthResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC Login request for email: {}", request.getEmail());

            // Convert gRPC request to DTO
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail(request.getEmail());
            authRequest.setPassword(request.getPassword());

            // Call service
            AuthResponse authResponse = authService.login(authRequest);

            // Convert response to proto
            com.wishkart.grpc.auth.AuthResponse response = ProtoConverter.toAuthResponse(authResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void refreshToken(RefreshTokenRequest request,
                             StreamObserver<com.wishkart.grpc.auth.AuthResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC RefreshToken request");

            // Call service
            AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());

            // Convert response to proto
            com.wishkart.grpc.auth.AuthResponse response = ProtoConverter.toAuthResponse(authResponse);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request,
                               StreamObserver<com.wishkart.grpc.auth.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC ForgotPassword request for email: {}", request.getEmail());

            // Call service
            authService.requestPasswordReset(request.getEmail());

            // Build response
            com.wishkart.grpc.auth.ApiResponse response = ProtoConverter.toAuthApiResponse(true,
                "Password reset link sent to your email");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request,
                              StreamObserver<com.wishkart.grpc.auth.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC ResetPassword request");

            // Call service
            authService.resetPassword(request.getToken(), request.getNewPassword());

            // Build response
            com.wishkart.grpc.auth.ApiResponse response = ProtoConverter.toAuthApiResponse(true,
                "Password reset successful");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request,
                            StreamObserver<com.wishkart.grpc.auth.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC VerifyEmail request");

            // Call service
            authService.verifyEmail(request.getToken());

            // Build response
            com.wishkart.grpc.auth.ApiResponse response = ProtoConverter.toAuthApiResponse(true,
                "Email verified successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
