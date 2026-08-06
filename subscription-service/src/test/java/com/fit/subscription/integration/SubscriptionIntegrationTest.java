package com.fit.subscription.integration;

import com.fit.subscription.entity.Plan;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.*;
import com.fit.subscription.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SubscriptionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionAddOnRepository subscriptionAddOnRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlanRepository planRepository;

    @BeforeEach
    void clearDatabase() {
        paymentRepository.deleteAll();
        subscriptionAddOnRepository.deleteAll();
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createSubscription_ShouldPersistSubscriptionAndPayment() throws Exception {
        User user = userRepository.save(new User(null, "Subscriber", "subscriber@example.com", "password123", UserRole.USER, BigDecimal.ZERO));
        Plan plan = planRepository.save(plan("Starter", BigDecimal.valueOf(499.99)));

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"planId":%d,"paymentMethod":"CARD","autoRenew":true,"addOns":[]}
                                """.formatted(user.getId(), plan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Subscriber"))
                .andExpect(jsonPath("$.planName").value("Starter"));

        Subscription saved = subscriptionRepository.findAll().get(0);
        assertEquals(plan.getId(), saved.getPlan().getId());
        assertTrue(paymentRepository.findBySubscriptionId(saved.getId()).size() == 1);
    }

    @Test
    void cancelSubscription_ShouldPersistCancelledStatus() throws Exception {
        User user = userRepository.save(new User(null, "Subscriber", "cancel@example.com", "password123", UserRole.USER, BigDecimal.ZERO));
        Plan plan = planRepository.save(plan("Starter", BigDecimal.valueOf(499.99)));
        Subscription subscription = activeSubscription(user, plan);

        mockMvc.perform(put("/subscriptions/{id}/cancel", subscription.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.autoRenew").value(false));

        assertEquals(SubscriptionStatus.CANCELLED,
                subscriptionRepository.findById(subscription.getId()).orElseThrow().getStatus());
    }

    @Test
    void createSubscription_WithUnknownUser_ShouldReturnNotFound() throws Exception {
        Plan plan = planRepository.save(plan("Starter", BigDecimal.valueOf(499.99)));

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":9999,"planId":%d,"paymentMethod":"CARD","addOns":[]}
                                """.formatted(plan.getId())))
                .andExpect(status().isNotFound());
    }

    private Plan plan(String name, BigDecimal price) {
        return new Plan(null, name, price, 30, "Integration plan", PlanType.BASIC, true);
    }

    private Subscription activeSubscription(User user, Plan plan) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(true);
        subscription.setFinalPrice(plan.getPrice());
        subscription.setRenewalAttempts(0);
        return subscriptionRepository.save(subscription);
    }
}
