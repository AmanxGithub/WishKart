package com.wishkart.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishkart.dto.*;
import com.wishkart.entity.User;
import com.wishkart.repository.CartRepository;
import com.wishkart.repository.OrderRepository;
import com.wishkart.repository.ProductRepository;
import com.wishkart.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the complete e-commerce flow:
 * Register -> Browse Products -> Add to Cart -> Checkout -> View Orders
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E-Commerce Flow Integration Tests")
@EnableAutoConfiguration(exclude = {
    net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration.class,
    net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.class,
    net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class,
    net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration.class
})
class EcommerceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    private static String accessToken;
    private static Long userId;
    private static Long productId;

    @Test
    @Order(1)
    @DisplayName("Step 1: Register a new user")
    void step1_registerUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
            .firstName("Integration")
            .lastName("Tester")
            .email("integration.test@example.com")
            .password("TestPassword123!")
            .confirmPassword("TestPassword123!")
            .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.user.email").value("integration.test@example.com"))
            .andReturn();

        // Extract token and user ID for subsequent tests
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(responseBody,
            objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));

        accessToken = response.getData().getAccessToken();
        userId = response.getData().getUser().getId();

        assertThat(accessToken).isNotNull();
        assertThat(userId).isNotNull();

        // Verify user was created in database
        User user = userRepository.findById(userId).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("integration.test@example.com");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Browse products")
    void step2_browseProducts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Verify products exist (from DataInitializer)
        assertThat(responseBody).contains("content");

        // Get first product ID for cart test
        // Note: This assumes DataInitializer has created products
        if (productRepository.count() > 0) {
            productId = productRepository.findAll().get(0).getId();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: View featured products")
    void step3_viewFeaturedProducts() throws Exception {
        mockMvc.perform(get("/api/products/featured"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Search for products")
    void step4_searchProducts() throws Exception {
        mockMvc.perform(get("/api/products/search")
                .param("q", "phone"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: View categories")
    void step5_viewCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Add item to cart (requires authentication)")
    void step6_addToCart() throws Exception {
        if (productId == null) {
            // Skip if no products available
            return;
        }

        AddToCartRequest request = AddToCartRequest.builder()
            .productId(productId)
            .quantity(2)
            .build();

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: View cart")
    void step7_viewCart() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Update cart item quantity")
    void step8_updateCartQuantity() throws Exception {
        if (productId == null) return;

        mockMvc.perform(put("/api/cart/update/" + productId)
                .header("Authorization", "Bearer " + accessToken)
                .param("quantity", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Get cart item count")
    void step9_getCartCount() throws Exception {
        mockMvc.perform(get("/api/cart/count")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: View user profile")
    void step10_viewProfile() throws Exception {
        mockMvc.perform(get("/api/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("integration.test@example.com"));
    }

    @Test
    @Order(11)
    @DisplayName("Step 11: Verify unauthenticated access is blocked")
    void step11_verifySecurityBlocks() throws Exception {
        // Try to access cart without token
        mockMvc.perform(get("/api/cart"))
            .andExpect(status().isUnauthorized());

        // Try to access profile without token
        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isUnauthorized());

        // Try to access admin endpoint without admin role
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("Step 12: Clear cart")
    void step12_clearCart() throws Exception {
        mockMvc.perform(delete("/api/cart/clear")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify cart is empty
        mockMvc.perform(get("/api/cart/count")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @Order(13)
    @DisplayName("Step 13: Refresh authentication token")
    void step13_refreshToken() throws Exception {
        // Login to get a refresh token
        AuthRequest loginRequest = AuthRequest.builder()
            .email("integration.test@example.com")
            .password("TestPassword123!")
            .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        assertThat(loginResponse).contains("refreshToken");
    }

    @Test
    @Order(99)
    @DisplayName("Cleanup: Logout user")
    void cleanup_logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }
}
