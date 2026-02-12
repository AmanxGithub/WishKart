package com.wishkart.service;

import com.wishkart.dto.*;
import com.wishkart.entity.Cart;
import com.wishkart.entity.User;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CartRepository;
import com.wishkart.repository.UserRepository;
import com.wishkart.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create user
        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail().toLowerCase())
            .password(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .role(User.Role.CUSTOMER)
            .enabled(true)
            .emailVerified(false)
            .verificationToken(UUID.randomUUID().toString())
            .build();

        user = userRepository.save(user);

        // Create empty cart for user
        Cart cart = Cart.builder()
            .user(user)
            .build();
        cartRepository.save(cart);

        log.info("New user registered: {}", user.getEmail());

        // Generate tokens
        String accessToken = tokenProvider.generateToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.of(
            accessToken,
            refreshToken,
            tokenProvider.getExpirationInSeconds(),
            UserDTO.fromEntity(user)
        );
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().toLowerCase(),
                request.getPassword()
            )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled. Please contact support.");
        }

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.of(
            accessToken,
            refreshToken,
            tokenProvider.getExpirationInSeconds(),
            UserDTO.fromEntity(user)
        );
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String newAccessToken = tokenProvider.generateToken(email);
        String newRefreshToken = tokenProvider.generateRefreshToken(email);

        return AuthResponse.of(
            newAccessToken,
            newRefreshToken,
            tokenProvider.getExpirationInSeconds(),
            UserDTO.fromEntity(user)
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setResetPasswordToken(UUID.randomUUID().toString());
        userRepository.save(user);

        // TODO: Send password reset email
        log.info("Password reset requested for: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        userRepository.save(user);

        log.info("Password reset completed for: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified for: {}", user.getEmail());
    }
}
