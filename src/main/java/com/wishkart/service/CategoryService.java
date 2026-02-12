package com.wishkart.service;

import com.wishkart.dto.CategoryDTO;
import com.wishkart.entity.Category;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CategoryRepository;
import com.wishkart.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for category management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return CategoryDTO.fromEntityWithSubcategories(category);
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return CategoryDTO.fromEntityWithSubcategories(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllActiveOrdered().stream()
            .map(CategoryDTO::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getRootCategories() {
        return categoryRepository.findRootCategories().stream()
            .map(CategoryDTO::fromEntityWithSubcategories)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubcategories(Long parentId) {
        return categoryRepository.findSubcategories(parentId).stream()
            .map(CategoryDTO::fromEntity)
            .toList();
    }

    @Transactional
    public CategoryDTO createCategory(String name, String description, String imageUrl, Long parentId, Integer displayOrder) {
        // Validate name uniqueness
        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Category with name '" + name + "' already exists");
        }

        // Generate slug
        String slug = SlugUtil.generateSlug(name);
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = SlugUtil.generateSlug(name) + "-" + counter++;
        }

        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", parentId));
        }

        Category category = Category.builder()
            .name(name)
            .slug(slug)
            .description(description)
            .imageUrl(imageUrl)
            .parent(parent)
            .displayOrder(displayOrder != null ? displayOrder : 0)
            .active(true)
            .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getName());
        return CategoryDTO.fromEntity(category);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, String name, String description, String imageUrl, Long parentId, Integer displayOrder, Boolean active) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Update name and slug if changed
        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.existsByName(name)) {
                throw new BadRequestException("Category with name '" + name + "' already exists");
            }
            category.setName(name);
            String slug = SlugUtil.generateSlug(name);
            int counter = 1;
            while (categoryRepository.existsBySlug(slug) && !slug.equals(category.getSlug())) {
                slug = SlugUtil.generateSlug(name) + "-" + counter++;
            }
            category.setSlug(slug);
        }

        if (description != null) {
            category.setDescription(description);
        }
        if (imageUrl != null) {
            category.setImageUrl(imageUrl);
        }
        if (displayOrder != null) {
            category.setDisplayOrder(displayOrder);
        }
        if (active != null) {
            category.setActive(active);
        }

        // Update parent
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", parentId));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Category updated: {}", category.getName());
        return CategoryDTO.fromEntity(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getProducts().isEmpty()) {
            throw new BadRequestException("Cannot delete category with products. Please reassign or delete products first.");
        }

        if (!category.getSubcategories().isEmpty()) {
            throw new BadRequestException("Cannot delete category with subcategories. Please delete subcategories first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
    }

    @Transactional(readOnly = true)
    public long countCategories() {
        return categoryRepository.count();
    }
}
