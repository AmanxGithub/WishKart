package com.wishkart.dto;

import com.wishkart.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean active;
    private Integer displayOrder;
    private Long parentId;
    private String parentName;
    private List<CategoryDTO> subcategories;
    private int productCount;

    public static CategoryDTO fromEntity(Category category) {
        return CategoryDTO.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .imageUrl(category.getImageUrl())
            .active(category.isActive())
            .displayOrder(category.getDisplayOrder())
            .parentId(category.getParent() != null ? category.getParent().getId() : null)
            .parentName(category.getParent() != null ? category.getParent().getName() : null)
            .productCount(category.getProductCount())
            .build();
    }

    public static CategoryDTO fromEntityWithSubcategories(Category category) {
        CategoryDTO dto = fromEntity(category);
        dto.setSubcategories(
            category.getSubcategories().stream()
                .filter(Category::isActive)
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList())
        );
        return dto;
    }
}
