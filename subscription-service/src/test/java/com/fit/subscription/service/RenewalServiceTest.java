package com.fit.subscription.service;

import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.entity.*;
import com.fit.subscription.enums.*;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PaymentRepository;
import com.fit.subscription.repository.SubscriptionAddOnRepository;
import com.fit.subscription.repository.SubscriptionRepository;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenewalServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DunningService dunningService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SubscriptionAddOnRepository subscriptionAddOnRepository;

    @InjectMocks
    private RenewalService renewalService;

    @Test
    void renewSubscription_ShouldRenewSuccessfully() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(true);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now().minusDays(30));
        subscription.setEndDate(LocalDate.now());

        Plan plan = new Plan();
        plan.setPrice(BigDecimal.valueOf(1000));
        plan.setDurationDays(30);

        subscription.setPlan(plan);

        User user = new User();
        user.setId(1L);
        subscription.setUser(user);

        Payment lastPayment = new Payment();
        lastPayment.setPaymentMethod(PaymentMethod.CARD);

        AddOn addOn = new AddOn();
        addOn.setUnitPrice(BigDecimal.valueOf(100));

        SubscriptionAddOn subscriptionAddOn = new SubscriptionAddOn();
        subscriptionAddOn.setAddOn(addOn);
        subscriptionAddOn.setUnitsIncluded(2);

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO();
        paymentResponse.setPaymentStatus(PaymentStatus.SUCCESS);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(lastPayment));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of(subscriptionAddOn));

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(PaymentMethod.class),
                eq(PaymentType.RENEWAL)
        )).thenReturn(paymentResponse);

        renewalService.renewSubscription(subscriptionId);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());

        assertEquals(
                LocalDate.now().plusDays(1),
                subscription.getStartDate()
        );

        assertEquals(
                LocalDate.now().plusDays(31),
                subscription.getEndDate()
        );

        assertEquals(0, subscription.getRenewalAttempts());

        assertEquals(0, subscriptionAddOn.getUnitsUsed());

        verify(subscriptionRepository, atLeastOnce()).save(subscription);

        verify(subscriptionAddOnRepository).save(subscriptionAddOn);

        verify(dunningService).saveLog(
                eq(subscription),
                eq(1),
                eq(PaymentStatus.SUCCESS),
                isNull(),
                isNull()
        );
    }
    @Test
    void renewSubscription_ShouldThrow_WhenSubscriptionNotFound() {

        Long subscriptionId = 1L;

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> renewalService.renewSubscription(subscriptionId)
        );

        verify(paymentRepository, never()).findTopBySubscriptionOrderByPaymentDateDesc(any());

        verify(paymentService, never()).processPayment(
                anyLong(),
                any(),
                any(),
                any()
        );
    }
    @Test
    void renewSubscription_ShouldThrow_WhenAutoRenewDisabled() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(false);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.renewSubscription(subscriptionId)
                );

        assertEquals(
                "Auto renewal is not allowed",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .findTopBySubscriptionOrderByPaymentDateDesc(any());

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void renewSubscription_ShouldThrow_WhenSubscriptionNotActive() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(true);
        subscription.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.renewSubscription(subscriptionId)
                );

        assertEquals(
                "Subscription is not Active to enable Renewal",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .findTopBySubscriptionOrderByPaymentDateDesc(any());

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void renewSubscription_ShouldThrow_WhenRenewDateDoesNotMatch() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(true);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // Tomorrow instead of today
        subscription.setEndDate(LocalDate.now().plusDays(1));

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.renewSubscription(subscriptionId)
                );

        assertEquals(
                "Renew Date does not match",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .findTopBySubscriptionOrderByPaymentDateDesc(any());

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void renewSubscription_ShouldThrow_WhenPreviousPaymentNotFound() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(true);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(LocalDate.now());

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> renewalService.renewSubscription(subscriptionId)
                );

        assertEquals(
                "No previous payment found",
                exception.getMessage()
        );

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void renewSubscription_ShouldMoveToGrace_WhenPaymentFails() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setAutoRenew(true);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEndDate(LocalDate.now());

        User user = new User();
        subscription.setUser(user);

        Plan plan = new Plan();
        plan.setPrice(BigDecimal.valueOf(1000));
        subscription.setPlan(plan);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.CARD);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentStatus(PaymentStatus.FAILED);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of());

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.RENEWAL)))
                .thenReturn(response);

        renewalService.renewSubscription(subscriptionId);

        assertEquals(SubscriptionStatus.GRACE, subscription.getStatus());

        assertEquals(
                LocalDate.now().plusDays(1),
                subscription.getNextRetryDate()
        );

        assertEquals(
                LocalDate.now().plusDays(3),
                subscription.getGraceEndDate()
        );

        verify(notificationService)
                .sendDunningEmail(
                        eq(user),
                        eq(1),
                        eq(LocalDate.now().plusDays(1))
                );

        verify(dunningService)
                .saveLog(
                        eq(subscription),
                        eq(1),
                        eq(PaymentStatus.FAILED),
                        eq("Renewal payment failed"),
                        eq(LocalDate.now().plusDays(1))
                );
    }
    @Test
    void retryPayment_ShouldRenewSuccessfully() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now());
        subscription.setRenewalAttempts(1);

        User user = new User();
        subscription.setUser(user);

        Plan plan = new Plan();
        plan.setPrice(BigDecimal.valueOf(1000));
        plan.setDurationDays(30);

        subscription.setPlan(plan);
        subscription.setEndDate(LocalDate.now());

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.CARD);

        AddOn addOn = new AddOn();
        addOn.setUnitPrice(BigDecimal.valueOf(100));

        SubscriptionAddOn subscriptionAddOn = new SubscriptionAddOn();
        subscriptionAddOn.setAddOn(addOn);
        subscriptionAddOn.setUnitsIncluded(2);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentStatus(PaymentStatus.SUCCESS);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of(subscriptionAddOn));

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.RENEWAL)))
                .thenReturn(response);

        renewalService.retryPayment(subscriptionId);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());

        assertEquals(0, subscription.getRenewalAttempts());

        assertTrue(subscription.getAutoRenew());

        assertNull(subscription.getGraceEndDate());

        assertNull(subscription.getNextRetryDate());

        assertEquals(0, subscriptionAddOn.getUnitsUsed());

        verify(subscriptionRepository, atLeastOnce())
                .save(subscription);

        verify(subscriptionAddOnRepository)
                .save(subscriptionAddOn);
    }
    @Test
    void retryPayment_ShouldThrow_WhenSubscriptionNotGrace() {

        Subscription subscription = new Subscription();

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.retryPayment(1L)
                );

        assertEquals(
                "Cannot proceed with payment as it is not Grace",
                exception.getMessage()
        );

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void retryPayment_ShouldThrow_WhenRetryDateDoesNotMatch() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now().plusDays(1));

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.retryPayment(subscriptionId)
                );

        assertEquals(
                "Retry Payment date does not match",
                exception.getMessage()
        );

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void retryPayment_ShouldThrow_WhenMaximumRetryAttemptsReached() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();

        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now());
        subscription.setRenewalAttempts(3);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> renewalService.retryPayment(subscriptionId)
                );

        assertEquals(
                "Maximum retry attempts reached",
                exception.getMessage()
        );

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void retryPayment_ShouldExpireImmediately_WhenGracePeriodHasPassed_EvenIfAttemptsUnder3() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now());
        subscription.setRenewalAttempts(1);
        // grace deadline already passed, even though attempts (1) is still under the max (3)
        subscription.setGraceEndDate(LocalDate.now().minusDays(1));

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        renewalService.retryPayment(subscriptionId);

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        assertFalse(subscription.getAutoRenew());
        assertNull(subscription.getNextRetryDate());

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
        verify(subscriptionRepository).save(subscription);
    }
    @Test
    void retryPayment_ShouldExpireSubscription_WhenThirdRetryFails() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();

        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now());
        subscription.setRenewalAttempts(2);

        User user = new User();
        subscription.setUser(user);

        Plan plan = new Plan();
        plan.setPrice(BigDecimal.valueOf(1000));

        subscription.setPlan(plan);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.CARD);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentStatus(PaymentStatus.FAILED);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository
                .findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of());

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.RENEWAL)))
                .thenReturn(response);

        renewalService.retryPayment(subscriptionId);

        assertEquals(
                SubscriptionStatus.EXPIRED,
                subscription.getStatus()
        );

        assertFalse(subscription.getAutoRenew());

        assertNull(subscription.getNextRetryDate());

        assertNull(subscription.getGraceEndDate());

        assertEquals(3,
                subscription.getRenewalAttempts());

        verify(notificationService)
                .sendDunningEmail(
                        eq(user),
                        eq(3),
                        isNull()
                );

        verify(dunningService)
                .saveLog(
                        eq(subscription),
                        eq(3),
                        eq(PaymentStatus.FAILED),
                        eq("Maximum retry attempts reached"),
                        isNull()
                );
    }
    @Test
    void retryPayment_ShouldIncrementAttempt_WhenRetryFails() {

        Long subscriptionId = 1L;

        Subscription subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(SubscriptionStatus.GRACE);
        subscription.setNextRetryDate(LocalDate.now());
        subscription.setRenewalAttempts(1);

        User user = new User();
        subscription.setUser(user);

        Plan plan = new Plan();
        plan.setPrice(BigDecimal.valueOf(1000));
        subscription.setPlan(plan);

        Payment payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.CARD);

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentStatus(PaymentStatus.FAILED);

        when(subscriptionRepository.findById(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of());

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.RENEWAL)))
                .thenReturn(response);

        renewalService.retryPayment(subscriptionId);

        assertEquals(2, subscription.getRenewalAttempts());

        assertEquals(
                SubscriptionStatus.GRACE,
                subscription.getStatus()
        );

        assertEquals(
                LocalDate.now().plusDays(1),
                subscription.getNextRetryDate()
        );

        verify(notificationService)
                .sendDunningEmail(
                        eq(user),
                        eq(2),
                        eq(LocalDate.now().plusDays(1))
                );

        verify(dunningService)
                .saveLog(
                        eq(subscription),
                        eq(2),
                        eq(PaymentStatus.FAILED),
                        eq("Retry payment failed again"),
                        eq(LocalDate.now().plusDays(1))
                );
    }

}
