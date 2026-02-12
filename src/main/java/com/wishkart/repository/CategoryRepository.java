package com.wishkart.repository;

import com.wishkart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Category> findByActiveTrue();

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = true ORDER BY c.displayOrder")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.active = true ORDER BY c.displayOrder")
    List<Category> findSubcategories(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.displayOrder")
    List<Category> findAllActiveOrdered();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.parent IS NULL AND c.active = true")
    List<Category> findRootCategoriesWithSubcategories();
}
