package com.wishkart.repository;

import com.wishkart.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeIgnoreCase(String code);

    Optional<Coupon> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);

    Page<Coupon> findByActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.startDate <= :now AND c.endDate >= :now")
    Page<Coupon> findValidCoupons(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT c FROM Coupon c WHERE c.endDate < :now OR (c.usageLimit IS NOT NULL AND c.usageCount >= c.usageLimit)")
    Page<Coupon> findExpiredCoupons(@Param("now") LocalDateTime now, Pageable pageable);
}
