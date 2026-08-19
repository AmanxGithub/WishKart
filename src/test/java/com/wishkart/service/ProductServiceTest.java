package com.wishkart.service;

import com.wishkart.dto.CreateProductRequest;
import com.wishkart.dto.ProductDTO;
import com.wishkart.entity.Category;
import com.wishkart.entity.Product;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CategoryRepository;
import com.wishkart.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .build();
        testCategory.setId(1L);

        testProduct = Product.builder()
            .name("Test Product")
            .slug("test-product")
            .sku("SKU-TEST-001")
            .description("Test description")
            .price(new BigDecimal("99.99"))
            .stockQuantity(100)
            .active(true)
            .featured(false)
            .category(testCategory)
            .build();
        testProduct.setId(1L);
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            ProductDTO result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Product");
            assertThat(result.getSku()).isEqualTo("SKU-TEST-001");
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void shouldThrowExceptionWhenNotFound() {
            when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getProductBySlug")
    class GetProductBySlug {

        @Test
        @DisplayName("should return product when found by slug")
        void shouldReturnProductWhenFoundBySlug() {
//            when(redisTemplate.opsForValue())
//                    .thenReturn(valueOperations);
//            when(valueOperations.get(any()))
//                    .thenReturn(null);
            when(productRepository.findBySlug("test-product"))
                .thenReturn(Optional.of(testProduct));

            ProductDTO result = productService.getProductBySlug("test-product");

            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("test-product");
        }

        @Test
        @DisplayName("should throw exception when slug not found")
        void shouldThrowExceptionWhenSlugNotFound() {
//            when(redisTemplate.opsForValue())
//                    .thenReturn(valueOperations);
//            when(valueOperations.get(any()))
//                    .thenReturn(null);
            when(productRepository.findBySlug(any()))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductBySlug("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return paginated products")
        void shouldReturnPaginatedProducts() {
            List<Product> products = Arrays.asList(testProduct);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(productPage);

            Page<ProductDTO> result = productService.getAllProducts(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product successfully")
        void shouldCreateProductSuccessfully() {
            CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sku("SKU-NEW-001")
                .description("New description")
                .price(new BigDecimal("149.99"))
                .stockQuantity(50)
                .categoryId(1L)
                .build();

            when(productRepository.existsBySku("SKU-NEW-001")).thenReturn(false);
            when(productRepository.existsBySlug(any())).thenReturn(false);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            ProductDTO result = productService.createProduct(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Product");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sku("SKU-NEW-001")
                .categoryId(999L)
                .build();

            when(productRepository.existsBySku("SKU-NEW-001")).thenReturn(false);
            when(productRepository.existsBySlug(any())).thenReturn(false);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("should update stock quantity")
        void shouldUpdateStockQuantity() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.updateStock(1L, 150);

            verify(productRepository).save(argThat(product ->
                product.getStockQuantity() == 150
            ));
        }
    }

    @Nested
    @DisplayName("getFeaturedProducts")
    class GetFeaturedProducts {

        @Test
        @DisplayName("should return featured products")
        void shouldReturnFeaturedProducts() {
            testProduct.setFeatured(true);
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findFeaturedProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(testProduct)));

            Page<ProductDTO> result = productService.getFeaturedProducts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isFeatured()).isTrue();
        }
    }

    @Nested
    @DisplayName("searchProducts")
    class SearchProducts {

        @Test
        @DisplayName("should search products by keyword")
        void shouldSearchProductsByKeyword() {
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.searchProducts(eq("test"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(testProduct)));

            Page<ProductDTO> result = productService.searchProducts("test", pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(productRepository).searchProducts(eq("test"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getLowStockProducts")
    class GetLowStockProducts {

        @Test
        @DisplayName("should return low stock products")
        void shouldReturnLowStockProducts() {
            testProduct.setStockQuantity(5);
            testProduct.setLowStockThreshold(10);

            when(productRepository.findLowStockProducts())
                .thenReturn(Arrays.asList(testProduct));

            List<ProductDTO> result = productService.getLowStockProducts();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product")
        void shouldDeleteProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            doNothing().when(productRepository).delete(any(Product.class));

            productService.deleteProduct(1L);

            verify(productRepository).delete(any(Product.class));
        }
    }
}
