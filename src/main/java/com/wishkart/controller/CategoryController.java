package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.CategoryDTO;
import com.wishkart.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for category endpoints.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category operations")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories with subcategories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getRootCategories() {
        List<CategoryDTO> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable("id") Long id) {
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryBySlug(@PathVariable("slug") String slug) {
        CategoryDTO category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/{parentId}/subcategories")
    @Operation(summary = "Get subcategories of a category")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getSubcategories(@PathVariable("parentId") Long parentId) {
        List<CategoryDTO> subcategories = categoryService.getSubcategories(parentId);
        return ResponseEntity.ok(ApiResponse.success(subcategories));
    }
}
