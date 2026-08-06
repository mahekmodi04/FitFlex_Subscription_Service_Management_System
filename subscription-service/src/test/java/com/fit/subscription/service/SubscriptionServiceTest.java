package com.fit.subscription.service;

import com.fit.subscription.dto.*;
import com.fit.subscription.entity.*;
import com.fit.subscription.enums.*;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.CouponRepository;
import com.fit.subscription.repository.CouponUsageRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private CouponService couponService;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private AddOnService addOnService;

    @Mock
    private SubscriptionAddOnRepository subscriptionAddOnRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;
    private Plan plan;
    private Subscription subscription;
    private PaymentResponseDTO paymentResponse;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setName("Mahek");

        plan = new Plan();
        plan.setId(1L);
        plan.setName("Gold");
        plan.setPrice(new BigDecimal("1000"));
        plan.setDurationDays(30);

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));

        paymentResponse = new PaymentResponseDTO();
        paymentResponse.setPaymentStatus(PaymentStatus.SUCCESS);
    }
    @Test
    void createSubscription_success_withoutCoupon() {

        // ---------- Arrange ----------

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setUserId(1L);
        request.setPlanId(1L);
        request.setCouponCode(null);
        request.setAutoRenew(true);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setAddOns(new ArrayList<>());

        when(userService.getUserById(1L)).thenReturn(user);

        when(planService.getPlanById(1L)).thenReturn(plan);

        when(addOnService.calculateAddOnPrice(anyList()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription s = invocation.getArgument(0);

                    if (s.getId() == null) {
                        s.setId(1L);
                    }

                    return s;
                });

        when(paymentService.processPayment(
                eq(1L),
                eq(new BigDecimal("1000")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        )).thenReturn(paymentResponse);

        // ---------- Act ----------

        SubscriptionResponseDTO response =
                subscriptionService.createSubscription(request);

        // ---------- Assert ----------

        assertNotNull(response);

        assertEquals(1L, response.getId());

        assertEquals("Mahek", response.getUserName());

        assertEquals("Gold", response.getPlanName());

        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());

        assertEquals(new BigDecimal("1000"), response.getFinalPrice());

        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());

        verify(userService).getUserById(1L);

        verify(planService).getPlanById(1L);

        verify(paymentService).processPayment(
                eq(1L),
                eq(new BigDecimal("1000")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        );

        verify(addOnService)
                .attachAddOnsDuringSubscriptionCreation(any(), anyList());

        verify(subscriptionRepository, times(2))
                .save(any(Subscription.class));
    }
    @Test
    void createSubscription_paymentFails_shouldRemainPending() {

        // ---------- Arrange ----------

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setUserId(1L);
        request.setPlanId(1L);
        request.setCouponCode(null);
        request.setAutoRenew(true);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setAddOns(new ArrayList<>());

        when(userService.getUserById(1L))
                .thenReturn(user);

        when(planService.getPlanById(1L))
                .thenReturn(plan);

        when(addOnService.calculateAddOnPrice(anyList()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription s = invocation.getArgument(0);

                    if (s.getId() == null) {
                        s.setId(1L);
                    }

                    return s;
                });

        PaymentResponseDTO failedPayment = new PaymentResponseDTO();
        failedPayment.setPaymentStatus(PaymentStatus.FAILED);

        when(paymentService.processPayment(
                eq(1L),
                eq(new BigDecimal("1000")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        )).thenReturn(failedPayment);

        // ---------- Act ----------

        SubscriptionResponseDTO response =
                subscriptionService.createSubscription(request);

        // ---------- Assert ----------

        assertNotNull(response);

        assertEquals(SubscriptionStatus.PENDING, response.getStatus());

        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());

        assertEquals(new BigDecimal("1000"), response.getFinalPrice());

        verify(paymentService).processPayment(
                eq(1L),
                eq(new BigDecimal("1000")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        );

        verify(subscriptionRepository, times(2))
                .save(any(Subscription.class));

        verify(addOnService, never())
                .attachAddOnsDuringSubscriptionCreation(any(), anyList());
    }
    @Test
    void createSubscription_userNotFound_shouldThrowException() {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setUserId(1L);
        request.setPlanId(1L);

        when(userService.getUserById(1L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> subscriptionService.createSubscription(request)
        );

        assertEquals("User not found", ex.getMessage());

        verify(userService).getUserById(1L);

        verifyNoInteractions(planService);
        verifyNoInteractions(paymentService);
    }
    @Test
    void createSubscription_planNotFound_shouldThrowException() {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setUserId(1L);
        request.setPlanId(1L);

        when(userService.getUserById(1L))
                .thenReturn(user);

        when(planService.getPlanById(1L))
                .thenThrow(new ResourceNotFoundException("Plan not found"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> subscriptionService.createSubscription(request)
        );

        assertEquals("Plan not found", ex.getMessage());

        verify(userService).getUserById(1L);

        verify(planService).getPlanById(1L);

        verifyNoInteractions(paymentService);
    }
    @Test
    void createSubscription_invalidCoupon_shouldThrowException() {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();

        request.setUserId(1L);
        request.setPlanId(1L);
        request.setCouponCode("SAVE50");
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setAddOns(new ArrayList<>());

        when(userService.getUserById(1L))
                .thenReturn(user);

        when(planService.getPlanById(1L))
                .thenReturn(plan);

        when(couponService.getCouponByCode("SAVE50"))
                .thenThrow(new ResourceNotFoundException("Coupon not found"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> subscriptionService.createSubscription(request)
        );

        assertEquals("Coupon not found", ex.getMessage());

        verify(paymentService, never())
                .processPayment(any(), any(), any(), any());
    }
    @Test
    void createSubscription_shouldIncludeAddOnPrice() {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();

        request.setUserId(1L);
        request.setPlanId(1L);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setCouponCode(null);
        request.setAddOns(new ArrayList<>());

        when(userService.getUserById(1L))
                .thenReturn(user);

        when(planService.getPlanById(1L))
                .thenReturn(plan);

        when(addOnService.calculateAddOnPrice(anyList()))
                .thenReturn(new BigDecimal("500"));

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {

                    Subscription s = invocation.getArgument(0);

                    if (s.getId() == null) {
                        s.setId(1L);
                    }

                    return s;
                });

        PaymentResponseDTO response = new PaymentResponseDTO();

        response.setPaymentStatus(PaymentStatus.SUCCESS);

        when(paymentService.processPayment(
                eq(1L),
                eq(new BigDecimal("1500")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        )).thenReturn(response);

        SubscriptionResponseDTO dto =
                subscriptionService.createSubscription(request);

        assertEquals(new BigDecimal("1500"), dto.getFinalPrice());

        verify(paymentService).processPayment(
                eq(1L),
                eq(new BigDecimal("1500")),
                eq(PaymentMethod.CARD),
                eq(PaymentType.SUBSCRIPTION),
                eq(BigDecimal.ZERO)
        );
    }
    @Test
    void cancelSubscription_success() {

        subscription.setStatus(SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        SubscriptionResponseDTO dto =
                subscriptionService.cancelSubscription(1L);

        assertEquals(SubscriptionStatus.CANCELLED, dto.getStatus());

        verify(subscriptionRepository).save(subscription);
    }
    @Test
    void cancelSubscription_notFound() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> subscriptionService.cancelSubscription(1L)
        );

        verify(subscriptionRepository, never()).save(any());
    }
    @Test
    void cancelSubscription_alreadyCancelled() {

        subscription.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.cancelSubscription(1L)
        );

        verify(subscriptionRepository, never()).save(any());
    }
    //change plan
    @Test
    void changePlan_ShouldUpgradeSuccessfully() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);
        request.setPaymentMethod(PaymentMethod.CARD);

        User user = new User();
        user.setName("Mahek");

        Plan oldPlan = new Plan();
        oldPlan.setId(1L);
        oldPlan.setName("Basic");
        oldPlan.setPrice(BigDecimal.valueOf(1000));
        oldPlan.setDurationDays(30);

        Plan newPlan = new Plan();
        newPlan.setId(2L);
        newPlan.setName("Premium");
        newPlan.setPrice(BigDecimal.valueOf(2000));
        newPlan.setDurationDays(30);

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUser(user);
        subscription.setPlan(oldPlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setAutoRenew(true);
        subscription.setStartDate(LocalDate.now().minusDays(10));
        subscription.setEndDate(LocalDate.now().plusDays(20));

        PaymentResponseDTO paymentResponse = new PaymentResponseDTO();
        paymentResponse.setPaymentStatus(PaymentStatus.SUCCESS);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(planService.getPlanById(2L))
                .thenReturn(newPlan);

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class)))
                .thenReturn(paymentResponse);

        when(addOnService.calculateAddOnPrice(any()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangePlanResponseDTO response =
                subscriptionService.changePlan(request);

        assertEquals("Premium", response.getNewPlanName());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());

        verify(subscriptionRepository).save(any());
        verify(addOnService).carryForwardAndMergeAddOnsOnUpgrade(eq(subscription), any());
        verify(paymentService).processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class));
    }
    @Test
    void changePlan_ShouldThrow_WhenSubscriptionNotFound() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.changePlan(request));

        verify(subscriptionRepository, never()).save(any());
    }
    @Test
    void changePlan_ShouldThrow_WhenSubscriptionInactive() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);

        Subscription subscription = new Subscription();
        subscription.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.changePlan(request));
    }
    @Test
    void changePlan_ShouldThrow_WhenDowngradeAttempted() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);

        Plan oldPlan = new Plan();
        oldPlan.setPrice(BigDecimal.valueOf(2000));

        Plan newPlan = new Plan();
        newPlan.setPrice(BigDecimal.valueOf(1000));

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPlan(oldPlan);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(planService.getPlanById(2L))
                .thenReturn(newPlan);

        assertThrows(IllegalArgumentException.class,
                () -> subscriptionService.changePlan(request));
    }
    @Test
    void changePlan_ShouldStaySameActivePlan_WhenPaymentFails() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);
        request.setPaymentMethod(PaymentMethod.CARD);

        Plan oldPlan = new Plan();
        oldPlan.setPrice(BigDecimal.valueOf(1000));
        oldPlan.setDurationDays(30);

        Plan newPlan = new Plan();
        newPlan.setPrice(BigDecimal.valueOf(2000));
        newPlan.setDurationDays(30);

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPlan(oldPlan);
        subscription.setStartDate(LocalDate.now().minusDays(5));
        subscription.setEndDate(LocalDate.now().plusDays(25));

        PaymentResponseDTO payment = new PaymentResponseDTO();
        payment.setPaymentStatus(PaymentStatus.FAILED);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(planService.getPlanById(2L))
                .thenReturn(newPlan);

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class)))
                .thenReturn(payment);

        when(addOnService.calculateAddOnPrice(any()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangePlanResponseDTO response = subscriptionService.changePlan(request);

        // old plan was already paid for - subscription should remain ACTIVE on the old plan,
        // only the response's paymentStatus reflects that the upgrade itself failed
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertEquals(subscription.getEndDate(), response.getNewEndDate());

        verify(addOnService, never()).carryForwardAndMergeAddOnsOnUpgrade(any(), any());
    }
    @Test
    void changePlan_ShouldCarryForwardAddOns_OnSuccessfulUpgrade() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);
        request.setPaymentMethod(PaymentMethod.CARD);

        User user = new User();
        user.setName("Mahek");

        Plan oldPlan = new Plan();
        oldPlan.setPrice(BigDecimal.valueOf(1000));
        oldPlan.setDurationDays(30);

        Plan newPlan = new Plan();
        newPlan.setName("Premium");
        newPlan.setPrice(BigDecimal.valueOf(2000));
        newPlan.setDurationDays(30);

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUser(user);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPlan(oldPlan);
        subscription.setStartDate(LocalDate.now().minusDays(10));
        subscription.setEndDate(LocalDate.now().plusDays(20));

        PaymentResponseDTO payment = new PaymentResponseDTO();
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(planService.getPlanById(2L))
                .thenReturn(newPlan);

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class)))
                .thenReturn(payment);

        when(addOnService.calculateAddOnPrice(any()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.changePlan(request);

        // add-on carry-forward/merge is now AddOnService's responsibility - see
        // AddOnServiceTest for the actual unused-units carry-forward math
        verify(addOnService).carryForwardAndMergeAddOnsOnUpgrade(eq(subscription), any());
    }
    @Test
    void getSubscriptionById_ShouldReturnMappedResponse() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponseDTO response = subscriptionService.getSubscriptionById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Mahek", response.getUserName());
        assertEquals("Gold", response.getPlanName());
    }

    @Test
    void getSubscriptionById_ShouldThrow_WhenNotFound() {

        when(subscriptionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> subscriptionService.getSubscriptionById(99L));
    }

    @Test
    void getSubscriptionsByUser_ShouldReturnAllForThatUser() {

        when(subscriptionRepository.findByUserId(1L))
                .thenReturn(List.of(subscription));

        var result = subscriptionService.getSubscriptionsByUser(1L);

        assertEquals(1, result.size());
        assertEquals("Gold", result.get(0).getPlanName());
    }

    @Test
    void changePlan_ShouldCallPaymentWithUpgradeType() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();
        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);
        request.setPaymentMethod(PaymentMethod.UPI);

        Plan oldPlan = new Plan();
        oldPlan.setPrice(BigDecimal.valueOf(1000));
        oldPlan.setDurationDays(30);

        Plan newPlan = new Plan();
        newPlan.setPrice(BigDecimal.valueOf(2000));
        newPlan.setDurationDays(30);

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        User user = new User();
        user.setName("Mahek");
        subscription.setUser(user);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPlan(oldPlan);
        subscription.setStartDate(LocalDate.now().minusDays(2));
        subscription.setEndDate(LocalDate.now().plusDays(28));

        PaymentResponseDTO payment = new PaymentResponseDTO();
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(planService.getPlanById(2L))
                .thenReturn(newPlan);

        when(paymentService.processPayment(
                anyLong(),
                any(BigDecimal.class),
                any(),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class)))
                .thenReturn(payment);

        when(addOnService.calculateAddOnPrice(any()))
                .thenReturn(BigDecimal.ZERO);

        when(subscriptionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        subscriptionService.changePlan(request);

        verify(paymentService).processPayment(
                anyLong(),
                any(BigDecimal.class),
                eq(PaymentMethod.UPI),
                eq(PaymentType.UPGRADE),
                any(BigDecimal.class));
    }
}
