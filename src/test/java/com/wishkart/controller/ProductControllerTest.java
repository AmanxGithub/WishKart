package com.wishkart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishkart.dto.CreateProductRequest;
import com.wishkart.dto.ProductDTO;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProductController Integration Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductDTO createTestProductDTO() {
        return ProductDTO.builder()
            .id(1L)
            .name("Test Product")
            .slug("test-product")
            .sku("SKU-001")
            .description("Test description")
            .price(new BigDecimal("99.99"))
            .stockQuantity(100)
            .active(true)
            .featured(false)
            .categoryId(1L)
            .categoryName("Electronics")
            .build();
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetAllProducts {

        @Test
        @DisplayName("should return paginated products")
        void shouldReturnPaginatedProducts() throws Exception {
            Page<ProductDTO> response = new PageImpl<>(Arrays.asList(createTestProductDTO()));

            when(productService.getAllProducts(any(Pageable.class)))
                .thenReturn(response);

            mockMvc.perform(get("/api/products")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductById {

        @Test
        @DisplayName("should return product by ID")
        void shouldReturnProductById() throws Exception {
            when(productService.getProductById(1L)).thenReturn(createTestProductDTO());

            mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Product"));
        }

        @Test
        @DisplayName("should return 404 for non-existent product")
        void shouldReturn404ForNonExistentProduct() throws Exception {
            when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product not found"));

            mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/products/slug/{slug}")
    class GetProductBySlug {

        @Test
        @DisplayName("should return product by slug")
        void shouldReturnProductBySlug() throws Exception {
            when(productService.getProductBySlug("test-product")).thenReturn(createTestProductDTO());

            mockMvc.perform(get("/api/products/slug/test-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("test-product"));
        }
    }

    @Nested
    @DisplayName("GET /api/products/featured")
    class GetFeaturedProducts {

        @Test
        @DisplayName("should return featured products")
        void shouldReturnFeaturedProducts() throws Exception {
            ProductDTO featuredProduct = createTestProductDTO();
            featuredProduct.setFeatured(true);

            Page<ProductDTO> response = new PageImpl<>(Arrays.asList(featuredProduct));

            when(productService.getFeaturedProducts(any(Pageable.class))).thenReturn(response);

            mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].featured").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/products/search")
    class SearchProducts {

        @Test
        @DisplayName("should search products by keyword")
        void shouldSearchProducts() throws Exception {
            Page<ProductDTO> response = new PageImpl<>(Arrays.asList(createTestProductDTO()));

            when(productService.searchProducts(eq("test"), any(Pageable.class)))
                .thenReturn(response);

            mockMvc.perform(get("/api/products/search")
                    .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/products/category/{categoryId}")
    class GetProductsByCategory {

        @Test
        @DisplayName("should return products by category")
        void shouldReturnProductsByCategory() throws Exception {
            Page<ProductDTO> response = new PageImpl<>(Arrays.asList(createTestProductDTO()));

            when(productService.getProductsByCategory(eq(1L), any(Pageable.class)))
                .thenReturn(response);

            mockMvc.perform(get("/api/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/products (Admin only)")
    class CreateProduct {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should create product when admin")
        void shouldCreateProductWhenAdmin() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sku("SKU-NEW-001")
                .description("Description")
                .price(new BigDecimal("149.99"))
                .stockQuantity(50)
                .categoryId(1L)
                .build();

            ProductDTO createdProduct = createTestProductDTO();
            createdProduct.setName("New Product");

            when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(createdProduct);

            mockMvc.perform(post("/api/admin/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 401 for unauthenticated users")
        void shouldReturn401ForUnauthenticated() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .build();

            mockMvc.perform(post("/api/admin/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("should return 403 for non-admin users")
        void shouldReturn403ForNonAdmin() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .build();

            mockMvc.perform(post("/api/admin/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/products/{id}/stock (Admin only)")
    class UpdateStock {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should update stock when admin")
        void shouldUpdateStockWhenAdmin() throws Exception {
            mockMvc.perform(put("/api/admin/products/1/stock")
                    .param("quantity", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/products/{id} (Admin only)")
    class DeleteProduct {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should delete product when admin")
        void shouldDeleteProductWhenAdmin() throws Exception {
            mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }
}
