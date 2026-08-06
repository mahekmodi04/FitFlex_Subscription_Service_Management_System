package com.fit.subscription.service;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = new Coupon();

        coupon.setId(1L);
        coupon.setCode("WELCOME50");
        coupon.setDiscountPercentage(BigDecimal.valueOf(50));
        coupon.setDiscountAmount(BigDecimal.ZERO);
        coupon.setExpiryDate(LocalDate.now().plusDays(30));
        coupon.setUsageLimit(100);
        coupon.setActive(true);
    }

    @Test
    void shouldCreateCouponSuccessfully() {

        when(couponRepository.findByCode(coupon.getCode()))
                .thenReturn(Optional.empty());

        when(couponRepository.save(coupon))
                .thenReturn(coupon);

        Coupon savedCoupon = couponService.createCoupon(coupon);

        assertNotNull(savedCoupon);
        assertEquals("WELCOME50", savedCoupon.getCode());

        verify(couponRepository).save(coupon);
    }

    @Test
    void shouldThrowExceptionWhenCouponCodeAlreadyExists() {

        when(couponRepository.findByCode(coupon.getCode()))
                .thenReturn(Optional.of(coupon));

        assertThrows(IllegalArgumentException.class,
                () -> couponService.createCoupon(coupon));

        verify(couponRepository, never()).save(any());
    }

    @Test
    void shouldReturnCouponById() {

        when(couponRepository.findById(1L))
                .thenReturn(Optional.of(coupon));

        Coupon result = couponService.getCouponById(1L);

        assertEquals("WELCOME50", result.getCode());
    }

    @Test
    void shouldThrowExceptionWhenCouponIdNotFound() {

        when(couponRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> couponService.getCouponById(1L));
    }

    @Test
    void shouldReturnAllCoupons() {

        when(couponRepository.findAll())
                .thenReturn(List.of(coupon));

        List<Coupon> coupons = couponService.getAllCoupons();

        assertEquals(1, coupons.size());
    }

    @Test
    void shouldDeleteCouponSuccessfully() {

        when(couponRepository.existsById(1L))
                .thenReturn(true);

        couponService.deleteCoupon(1L);

        verify(couponRepository).deleteById(1L);
    }

    @Test
    void shouldUpdateCouponSuccessfully() {

        Coupon updatedCoupon = new Coupon();

        updatedCoupon.setCode("NEW50");
        updatedCoupon.setDiscountPercentage(BigDecimal.valueOf(60));
        updatedCoupon.setDiscountAmount(BigDecimal.ZERO);
        updatedCoupon.setExpiryDate(LocalDate.now().plusDays(50));
        updatedCoupon.setUsageLimit(200);
        updatedCoupon.setActive(false);

        when(couponRepository.findById(1L))
                .thenReturn(Optional.of(coupon));

        when(couponRepository.save(any(Coupon.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Coupon result = couponService.updateCoupon(1L, updatedCoupon);

        assertEquals("NEW50", result.getCode());
        assertEquals(BigDecimal.valueOf(60), result.getDiscountPercentage());
        assertEquals(200, result.getUsageLimit());
        assertFalse(result.getActive());
    }
}
