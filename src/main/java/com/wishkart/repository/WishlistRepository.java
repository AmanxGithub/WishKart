package com.wishkart.repository;

import com.wishkart.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    @Query("SELECT COUNT(p) FROM Wishlist w JOIN w.products p WHERE w.user.id = :userId")
    int countItemsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Wishlist w JOIN w.products p WHERE w.user.id = :userId AND p.id = :productId")
    boolean isProductInWishlist(@Param("userId") Long userId, @Param("productId") Long productId);
}
