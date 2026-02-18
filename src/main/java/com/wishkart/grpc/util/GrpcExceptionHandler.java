package com.wishkart.grpc.util;

import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.InsufficientStockException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.exception.UnauthorizedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

/**
 * Centralized exception handling for gRPC services.
 * Maps application exceptions to appropriate gRPC status codes.
 */
@Slf4j
public final class GrpcExceptionHandler {

    private GrpcExceptionHandler() {}

    /**
     * Execute a gRPC operation with exception handling.
     */
    public static <T> void handle(GrpcOperation operation, StreamObserver<T> responseObserver) {
        try {
            operation.execute();
        } catch (StatusRuntimeException e) {
            // Already a gRPC exception, pass through
            log.error("gRPC StatusRuntimeException: {}", e.getStatus().getDescription());
            responseObserver.onError(e);
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (BadRequestException e) {
            log.error("Bad request: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (UnauthorizedException e) {
            log.error("Unauthorized: {}", e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (InsufficientStockException e) {
            log.error("Insufficient stock: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (BadCredentialsException e) {
            log.error("Bad credentials: {}", e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Invalid email or password")
                .asRuntimeException());
        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Authentication failed")
                .asRuntimeException());
        } catch (AccessDeniedException e) {
            log.error("Access denied: {}", e.getMessage());
            responseObserver.onError(Status.PERMISSION_DENIED
                .withDescription("You don't have permission to access this resource")
                .asRuntimeException());
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (IllegalStateException e) {
            log.error("Illegal state: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                .withDescription(e.getMessage())
                .asRuntimeException());
        } catch (Exception e) {
            log.error("Unexpected error in gRPC service", e);
            responseObserver.onError(Status.INTERNAL
                .withDescription("An unexpected error occurred. Please try again later.")
                .withCause(e)
                .asRuntimeException());
        }
    }

    @FunctionalInterface
    public interface GrpcOperation {
        void execute() throws Exception;
    }
}
