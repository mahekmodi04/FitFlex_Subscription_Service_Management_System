package com.fit.subscription.controller;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")

public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }
    // Create Coupon
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Coupon createCoupon(@Valid @RequestBody Coupon coupon) {
        return couponService.createCoupon(coupon);
    }
    // Get All Coupons
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Coupon> getAllCoupons() {
        return couponService.getAllCoupons();
    }
    // Get Coupon by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Coupon getCouponById(@PathVariable Long id) {
        return couponService.getCouponById(id);
    }

    // Get Coupon by Code - any authenticated user, so the checkout flow can preview a
    // coupon's discount before submitting (validation limits below still apply server-side)
    @GetMapping("/code/{code}")
    @PreAuthorize("isAuthenticated()")
    public Coupon getCouponByCode(@PathVariable String code) {
        return couponService.getCouponByCode(code);
    }

    // Update Coupon
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Coupon updateCoupon(@PathVariable Long id, @Valid @RequestBody Coupon updatedCoupon) {
        return couponService.updateCoupon(id, updatedCoupon);
    }

    // Delete Coupon
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return "Coupon deleted successfully";
    }
}
