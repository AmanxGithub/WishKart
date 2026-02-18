package com.wishkart.grpc.util;

import com.wishkart.grpc.interceptor.GrpcJwtAuthInterceptor;
import com.wishkart.security.CustomUserDetails;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Optional;

/**
 * Utility class for accessing security context in gRPC services.
 * Mirrors the functionality of SecurityUtil for REST.
 */
public final class GrpcSecurityUtil {

    private GrpcSecurityUtil() {}

    /**
     * Get the current authenticated user from gRPC context.
     */
    public static Optional<CustomUserDetails> getCurrentUser() {
        CustomUserDetails user = GrpcJwtAuthInterceptor.USER_CONTEXT_KEY.get();
        return Optional.ofNullable(user);
    }

    /**
     * Get the current user's ID.
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(CustomUserDetails::getId);
    }

    /**
     * Get the current user's email.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUser().map(CustomUserDetails::getEmail);
    }

    /**
     * Check if a user is authenticated.
     */
    public static boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * Check if the current user is an admin.
     */
    public static boolean isAdmin() {
        return getCurrentUser()
            .map(CustomUserDetails::isAdmin)
            .orElse(false);
    }

    /**
     * Get current user ID or throw UNAUTHENTICATED status.
     */
    public static Long requireAuthentication() {
        return getCurrentUserId()
            .orElseThrow(() -> new StatusRuntimeException(
                Status.UNAUTHENTICATED.withDescription("Authentication required")));
    }

    /**
     * Get current user or throw UNAUTHENTICATED status.
     */
    public static CustomUserDetails requireUser() {
        return getCurrentUser()
            .orElseThrow(() -> new StatusRuntimeException(
                Status.UNAUTHENTICATED.withDescription("Authentication required")));
    }

    /**
     * Require admin role or throw PERMISSION_DENIED status.
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new StatusRuntimeException(
                Status.PERMISSION_DENIED.withDescription("Admin access required"));
        }
    }

    /**
     * Require admin and return user ID.
     */
    public static Long requireAdminAndGetId() {
        requireAdmin();
        return requireAuthentication();
    }
}
