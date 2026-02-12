package com.wishkart.service;

import com.wishkart.dto.AuthRequest;
import com.wishkart.dto.AuthResponse;
import com.wishkart.dto.RegisterRequest;
import com.wishkart.entity.Cart;
import com.wishkart.entity.User;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CartRepository;
import com.wishkart.repository.UserRepository;
import com.wishkart.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .password("encodedPassword")
            .role(User.Role.CUSTOMER)
            .enabled(true)
            .emailVerified(true)
            .build();
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());
            when(jwtTokenProvider.generateToken(anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refreshToken");
            when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse result = authService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getUser().getEmail()).isEqualTo("john@example.com");
            verify(userRepository).save(any(User.class));
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("existing@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception when passwords don't match")
        void shouldThrowExceptionWhenPasswordsDontMatch() {
            RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("Password123!")
                .confirmPassword("DifferentPassword!")
                .build();

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Passwords do not match");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should login user successfully")
        void shouldLoginUserSuccessfully() {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refreshToken");
            when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse result = authService.login(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("wrongPassword")
                .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("should throw exception when user not found after authentication")
        void shouldThrowExceptionWhenUserNotFound() {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when account is disabled")
        void shouldThrowExceptionWhenAccountDisabled() {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

            testUser.setEnabled(false);
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("disabled");
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            when(jwtTokenProvider.validateToken("validRefreshToken")).thenReturn(true);
            when(jwtTokenProvider.getEmailFromToken("validRefreshToken")).thenReturn("john@example.com");
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateToken(anyString())).thenReturn("newAccessToken");
            when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("newRefreshToken");
            when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);

            AuthResponse result = authService.refreshToken("validRefreshToken");

            assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            when(jwtTokenProvider.validateToken("invalidToken")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("invalidToken"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
        }
    }

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("should request password reset successfully")
        void shouldRequestPasswordResetSuccessfully() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.requestPasswordReset("john@example.com");

            verify(userRepository).save(any(User.class));
            assertThat(testUser.getResetPasswordToken()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.requestPasswordReset("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            testUser.setResetPasswordToken("valid-token");
            when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.resetPassword("valid-token", "newPassword");

            verify(userRepository).save(any(User.class));
            assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
            assertThat(testUser.getResetPasswordToken()).isNull();
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            when(userRepository.findByResetPasswordToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("invalid-token", "newPassword"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            testUser.setVerificationToken("valid-token");
            testUser.setEmailVerified(false);
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            authService.verifyEmail("valid-token");

            verify(userRepository).save(any(User.class));
            assertThat(testUser.isEmailVerified()).isTrue();
            assertThat(testUser.getVerificationToken()).isNull();
        }

        @Test
        @DisplayName("should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            when(userRepository.findByVerificationToken("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid verification token");
        }
    }
}
