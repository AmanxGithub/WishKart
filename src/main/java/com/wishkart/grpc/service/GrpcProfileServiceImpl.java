package com.wishkart.grpc.service;

import com.wishkart.dto.UserDTO;
import com.wishkart.grpc.profile.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.GrpcSecurityUtil;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.UserService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC service implementation for Profile operations.
 * All methods require authentication.
 * Delegates to existing UserService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcProfileServiceImpl extends ProfileServiceGrpc.ProfileServiceImplBase {

    private final UserService userService;

    @Override
    public void getProfile(Empty request, StreamObserver<ProfileResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC GetProfile request for user: {}", userId);

            UserDTO user = userService.getUserById(userId);

            ProfileResponse response = ProfileResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Profile retrieved successfully")
                .setUser(ProtoConverter.toProfileUserInfo(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void updateProfile(UpdateProfileRequest request,
                              StreamObserver<ProfileResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC UpdateProfile request for user: {}", userId);

            UserDTO user = userService.updateProfile(
                userId,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()
            );

            ProfileResponse response = ProfileResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Profile updated successfully")
                .setUser(ProtoConverter.toProfileUserInfo(user))
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void changePassword(ChangePasswordRequest request,
                               StreamObserver<com.wishkart.grpc.profile.ApiResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            Long userId = GrpcSecurityUtil.requireAuthentication();
            log.debug("gRPC ChangePassword request for user: {}", userId);

            userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

            com.wishkart.grpc.profile.ApiResponse response = ProtoConverter.toProfileApiResponse(true, "Password changed successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
