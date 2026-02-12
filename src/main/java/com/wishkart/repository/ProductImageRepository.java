package com.wishkart.repository;

import com.wishkart.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImage> findByProductIdAndPrimaryTrue(Long productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.primary = false WHERE pi.product.id = :productId AND pi.id != :imageId")
    void clearPrimaryForProduct(@Param("productId") Long productId, @Param("imageId") Long imageId);

    void deleteByProductId(Long productId);
}
