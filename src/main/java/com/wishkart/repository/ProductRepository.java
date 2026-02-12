package com.wishkart.repository;

import com.wishkart.entity.Category;
import com.wishkart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategory(Category category, Pageable pageable);

    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.featured = true AND p.active = true")
    Page<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.stockQuantity = 0")
    List<Product> findOutOfStockProducts();

    @Query("SELECT p FROM Product p JOIN p.reviews r WHERE p.active = true " +
           "GROUP BY p ORDER BY AVG(r.rating) DESC")
    Page<Product> findTopRatedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN p.orderItems oi WHERE p.active = true " +
           "GROUP BY p ORDER BY COUNT(oi) DESC")
    Page<Product> findBestSellingProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.createdAt DESC")
    Page<Product> findNewArrivals(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.trackInventory = true AND p.stockQuantity <= p.lowStockThreshold")
    long countLowStockProducts();
}
