package com.wishkart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishkart.dto.AuthRequest;
import com.wishkart.dto.AuthResponse;
import com.wishkart.dto.RegisterRequest;
import com.wishkart.dto.UserDTO;
import com.wishkart.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterSuccessfully() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role("CUSTOMER")
                .build();

            AuthResponse response = AuthResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .user(userDTO)
                .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.user.email").value("john@example.com"));
        }

        @Test
        @DisplayName("should return 400 for invalid email")
        void shouldReturn400ForInvalidEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .build();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

            UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .email("john@example.com")
                .role("CUSTOMER")
                .build();

            AuthResponse response = AuthResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .user(userDTO)
                .build();

            when(authService.login(any(AuthRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }

        @Test
        @DisplayName("should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("wrongPassword")
                .build();

            when(authService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .email("john@example.com")
                .build();

            AuthResponse response = AuthResponse.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .user(userDTO)
                .build();

            when(authService.refreshToken(anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\": \"validRefreshToken\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
        }
    }
}
