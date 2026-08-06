package com.fit.subscription.integration;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CouponIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CouponRepository couponRepository;

    @BeforeEach
    void clearCoupons() {
        couponRepository.deleteAll();
    }

    @Test
    void createCoupon_ShouldPersistCoupon() throws Exception {
        mockMvc.perform(post("/coupons").contentType(MediaType.APPLICATION_JSON).content(couponJson("WELCOME10", 10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WELCOME10"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"));

        assertTrue(couponRepository.findByCode("WELCOME10").isPresent());
    }

    @Test
    void getCouponByCode_ShouldReturnPersistedCoupon() throws Exception {
        Coupon coupon = couponRepository.save(coupon("SAVE20", 20));

        mockMvc.perform(get("/coupons/code/{code}", coupon.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(coupon.getId()))
                .andExpect(jsonPath("$.code").value("SAVE20"));
    }

    @Test
    void updateCoupon_ShouldUpdateDatabaseRecord() throws Exception {
        Coupon coupon = couponRepository.save(coupon("SAVE10", 10));

        mockMvc.perform(put("/coupons/{id}", coupon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(couponJson("SAVE25", 25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE25"));

        assertEquals("SAVE25", couponRepository.findById(coupon.getId()).orElseThrow().getCode());
    }

    @Test
    void deleteCoupon_ShouldRemoveDatabaseRecord() throws Exception {
        Coupon coupon = couponRepository.save(coupon("REMOVE", 15));

        mockMvc.perform(delete("/coupons/{id}", coupon.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("Coupon deleted successfully"));

        assertTrue(couponRepository.findById(coupon.getId()).isEmpty());
    }

    @Test
    void createCoupon_WithDuplicateCode_ShouldReturnBadRequest() throws Exception {
        couponRepository.save(coupon("UNIQUE", 10));

        mockMvc.perform(post("/coupons").contentType(MediaType.APPLICATION_JSON).content(couponJson("UNIQUE", 20)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCouponById_WhenMissing_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/coupons/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    private Coupon coupon(String code, int percentage) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountPercentage(java.math.BigDecimal.valueOf(percentage));
        coupon.setUsageLimit(10);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        coupon.setExpiryDate(LocalDate.now().plusDays(30));
        coupon.setType(com.fit.subscription.enums.CouponType.PERCENTAGE);
        return coupon;
    }

    private String couponJson(String code, int percentage) {
        return """
                {"code":"%s","discountPercentage":%d,"usageLimit":10,"usedCount":0,"active":true,"expiryDate":"%s","type":"PERCENTAGE"}
                """.formatted(code, percentage, LocalDate.now().plusDays(30));
    }
}
