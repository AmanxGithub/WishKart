package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.PagedResponse;
import com.wishkart.dto.ProductDTO;
import com.wishkart.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for product endpoints.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog operations")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable("id") Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductBySlug(@PathVariable("slug") String slug) {
        ProductDTO product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDTO> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> searchProducts(
            @RequestParam("q") String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.searchProducts(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/filter/price")
    @Operation(summary = "Filter products by price range")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByPriceRange(
            @RequestParam("minPrice") BigDecimal minPrice,
            @RequestParam("maxPrice") BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getFeaturedProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getFeaturedProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrival products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getNewArrivals(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getNewArrivals(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best selling products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getBestSellers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getBestSellers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(products)));
    }
}
