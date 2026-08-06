package com.fit.subscription.repository;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.entity.CouponUsage;
import com.fit.subscription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponUsageRepository extends JpaRepository<CouponUsage,Long> {
    Optional<CouponUsage> findByUserAndCoupon(User user, Coupon coupon);
}
