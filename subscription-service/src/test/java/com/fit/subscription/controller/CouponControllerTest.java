package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.entity.Coupon;
import com.fit.subscription.enums.CouponType;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.service.CouponService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("Should create coupon successfully")
    void createCoupon_ShouldReturnCreatedCoupon() throws Exception {

        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("WELCOME50");
        coupon.setDiscountPercentage(BigDecimal.valueOf(50));
        coupon.setDiscountAmount(BigDecimal.ZERO);
        coupon.setUsageLimit(100);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        coupon.setExpiryDate(LocalDate.now().plusDays(30));
        coupon.setType(CouponType.PERCENTAGE);

        when(couponService.createCoupon(any(Coupon.class)))
                .thenReturn(coupon);

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("WELCOME50"))
                .andExpect(jsonPath("$.discountPercentage").value(50));

        verify(couponService, times(1))
                .createCoupon(any(Coupon.class));
    }
    @Test
    @DisplayName("Should return coupon by id")
    void getCouponById_ShouldReturnCoupon() throws Exception {

        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("WELCOME50");
        coupon.setDiscountPercentage(BigDecimal.valueOf(50));
        coupon.setDiscountAmount(BigDecimal.ZERO);
        coupon.setUsageLimit(100);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        coupon.setExpiryDate(LocalDate.now().plusDays(30));
        coupon.setType(CouponType.PERCENTAGE);

        when(couponService.getCouponById(1L))
                .thenReturn(coupon);

        mockMvc.perform(get("/coupons/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("WELCOME50"));

        verify(couponService, times(1))
                .getCouponById(1L);
    }
    @Test
    @DisplayName("Should return all coupons")
    void getAllCoupons_ShouldReturnCoupons() throws Exception {

        Coupon coupon1 = new Coupon();
        coupon1.setId(1L);
        coupon1.setCode("WELCOME50");
        coupon1.setDiscountPercentage(BigDecimal.valueOf(50));
        coupon1.setUsageLimit(100);
        coupon1.setExpiryDate(LocalDate.now().plusDays(30));
        coupon1.setType(CouponType.PERCENTAGE);
        coupon1.setActive(true);

        Coupon coupon2 = new Coupon();
        coupon2.setId(2L);
        coupon2.setCode("FLAT200");
        coupon2.setDiscountAmount(BigDecimal.valueOf(200));
        coupon2.setUsageLimit(50);
        coupon2.setExpiryDate(LocalDate.now().plusDays(60));
        coupon2.setType(CouponType.AMOUNT);
        coupon2.setActive(true);

        when(couponService.getAllCoupons())
                .thenReturn(List.of(coupon1, coupon2));

        mockMvc.perform(get("/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("WELCOME50"))
                .andExpect(jsonPath("$[1].code").value("FLAT200"));

        verify(couponService, times(1))
                .getAllCoupons();
    }
    @Test
    @DisplayName("Should return coupon by code")
    void getCouponByCode_ShouldReturnCoupon() throws Exception {

        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("WELCOME50");
        coupon.setDiscountPercentage(BigDecimal.valueOf(50));
        coupon.setUsageLimit(100);
        coupon.setExpiryDate(LocalDate.now().plusDays(30));
        coupon.setType(CouponType.PERCENTAGE);
        coupon.setActive(true);

        when(couponService.getCouponByCode("WELCOME50"))
                .thenReturn(coupon);

        mockMvc.perform(get("/coupons/code/{code}", "WELCOME50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELCOME50"));

        verify(couponService, times(1))
                .getCouponByCode("WELCOME50");
    }
    @Test
    @DisplayName("Should update coupon successfully")
    void updateCoupon_ShouldReturnUpdatedCoupon() throws Exception {

        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("UPDATED50");
        coupon.setDiscountPercentage(BigDecimal.valueOf(60));
        coupon.setUsageLimit(150);
        coupon.setExpiryDate(LocalDate.now().plusDays(45));
        coupon.setType(CouponType.PERCENTAGE);
        coupon.setActive(true);

        when(couponService.updateCoupon(any(Long.class), any(Coupon.class)))
                .thenReturn(coupon);

        mockMvc.perform(put("/coupons/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED50"))
                .andExpect(jsonPath("$.discountPercentage").value(60));

        verify(couponService, times(1))
                .updateCoupon(any(Long.class), any(Coupon.class));
    }
    @Test
    @DisplayName("Should delete coupon successfully")
    void deleteCoupon_ShouldReturnSuccessMessage() throws Exception {

        doNothing().when(couponService).deleteCoupon(1L);

        mockMvc.perform(delete("/coupons/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Coupon deleted successfully"));

        verify(couponService, times(1))
                .deleteCoupon(1L);
    }
    @Test
    @DisplayName("Should return BadRequest when validation fails")
    void createCoupon_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        Coupon coupon = new Coupon();

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return 404 when coupon is not found")
    void getCouponById_ShouldReturnNotFound() throws Exception {

        when(couponService.getCouponById(100L))
                .thenThrow(new ResourceNotFoundException("Coupon not found"));

        mockMvc.perform(get("/coupons/{id}", 100L))
                .andExpect(status().isNotFound());

        verify(couponService, times(1))
                .getCouponById(100L);
    }
}