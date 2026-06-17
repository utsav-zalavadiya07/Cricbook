package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByUserIdAndCouponId(Long userId, Long couponId);
}
