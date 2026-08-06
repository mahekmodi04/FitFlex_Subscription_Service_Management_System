package com.fit.subscription.integration;

import com.fit.subscription.entity.Payment;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlanRepository planRepository;

    @BeforeEach
    void clearDatabase() {
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getPaymentById_ShouldReturnPersistedPayment() throws Exception {
        Payment payment = paymentFor(subscription("one@example.com"), "txn-one");

        mockMvc.perform(get("/payments/{id}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.amount").value(499.99))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    void getPaymentsBySubscription_ShouldReturnOnlyMatchingPayments() throws Exception {
        Subscription first = subscription("first@example.com");
        Subscription second = subscription("second@example.com");
        paymentFor(first, "txn-first");
        paymentFor(second, "txn-second");

        mockMvc.perform(get("/payments/subscription/{subscriptionId}", first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("txn-first"));
    }

    @Test
    void getPaymentById_WhenMissing_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    private Subscription subscription(String email) {
        User user = new User(null, "Integration User", email, "password123", UserRole.USER, BigDecimal.ZERO);
        user = userRepository.save(user);
        Plan plan = new Plan(null, "Integration Plan " + email, BigDecimal.valueOf(499.99), 30,
                "Test plan", PlanType.BASIC, true);
        plan = planRepository.save(plan);
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(true);
        subscription.setFinalPrice(BigDecimal.valueOf(499.99));
        subscription.setRenewalAttempts(0);
        return subscriptionRepository.save(subscription);
    }

    private Payment paymentFor(Subscription subscription, String transactionId) {
        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(BigDecimal.valueOf(499.99));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentType(PaymentType.SUBSCRIPTION);
        payment.setTransactionId(transactionId);
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }
}
