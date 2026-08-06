package com.fit.subscription.integration;

import com.fit.subscription.entity.Plan;
import com.fit.subscription.enums.PlanType;
import com.fit.subscription.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository planRepository;

    @Test
    @DisplayName("Integration - Create Plan")
    void createPlan_ShouldSavePlanInDatabase() throws Exception {

        planRepository.deleteAll();

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Premium Plan",
                                  "price":999.99,
                                  "durationDays":30,
                                  "description":"Premium monthly subscription",
                                  "tier":"PREMIUM",
                                  "active":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium Plan"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.durationDays").value(30))
                .andExpect(jsonPath("$.tier").value("PREMIUM"));

        Plan savedPlan =
                planRepository.findByName("Premium Plan").orElse(null);

        assertNotNull(savedPlan);

        assertEquals("Premium Plan", savedPlan.getName());
        assertEquals(new BigDecimal("999.99"), savedPlan.getPrice());
        assertEquals(30, savedPlan.getDurationDays());
        assertEquals(PlanType.PREMIUM, savedPlan.getTier());
        assertTrue(savedPlan.getActive());
    }
}
