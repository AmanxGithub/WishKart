package com.wishkart.grpc.interceptor;

import com.wishkart.security.CustomUserDetails;
import com.wishkart.security.JwtTokenProvider;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * gRPC interceptor that mirrors REST JwtAuthenticationFilter.
 * Extracts JWT from metadata, validates, and sets SecurityContext.
 */
@GrpcGlobalServerInterceptor
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "grpc.server.port", matchIfMissing = false)
public class GrpcJwtAuthInterceptor implements ServerInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    // Metadata key for Authorization header
    public static final Metadata.Key<String> AUTHORIZATION_KEY =
        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    // Alternative key for just the token
    public static final Metadata.Key<String> TOKEN_KEY =
        Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    // Context key to pass user info to handlers
    public static final Context.Key<CustomUserDetails> USER_CONTEXT_KEY =
        Context.key("user");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.debug("gRPC call intercepted: {}", methodName);

        // Extract JWT token from metadata
        String token = extractToken(headers);

        Context context = Context.current();

        if (token != null && tokenProvider.validateToken(token)) {
            try {
                String email = tokenProvider.getEmailFromToken(token);
                CustomUserDetails userDetails =
                    (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                // Set Spring Security context
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Add to gRPC context for service access
                context = context.withValue(USER_CONTEXT_KEY, userDetails);

                log.debug("User authenticated via gRPC: {}", email);
            } catch (Exception e) {
                log.error("Failed to authenticate gRPC request", e);
            }
        }

        // Proceed with the call in the context
        return Contexts.interceptCall(context, call, headers, next);
    }

    private String extractToken(Metadata headers) {
        // Try Authorization header first (Bearer token)
        String bearerToken = headers.get(AUTHORIZATION_KEY);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Try direct token header
        String directToken = headers.get(TOKEN_KEY);
        if (directToken != null && !directToken.isEmpty()) {
            return directToken;
        }

        return null;
    }
}
