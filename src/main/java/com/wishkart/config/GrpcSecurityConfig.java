package com.wishkart.config;

import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC Security configuration.
 * Provides the required GrpcAuthenticationReader bean for gRPC server security integration.
 */
@Configuration
public class GrpcSecurityConfig {

    /**
     * Configures the authentication reader for gRPC requests.
     * Uses BasicGrpcAuthenticationReader which reads authentication from gRPC metadata.
     * 
     * @return the GrpcAuthenticationReader bean
     */
    @Bean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        // BasicGrpcAuthenticationReader reads Basic auth from gRPC metadata
        // For JWT-based auth, you could use BearerAuthenticationReader instead
        return new BasicGrpcAuthenticationReader();
    }
}
