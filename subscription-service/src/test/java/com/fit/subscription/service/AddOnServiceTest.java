package com.fit.subscription.service;

import com.fit.subscription.dto.AddOnRequestDTO;
import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.dto.SubscriptionAddOnResponseDTO;
import com.fit.subscription.entity.AddOn;
import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.SubscriptionAddOn;
import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.enums.SubscriptionStatus;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.AddOnRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddOnServiceTest {

    @Mock
    private AddOnRepository addOnRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionAddOnRepository subscriptionAddOnRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AddOnService addOnService;

    private AddOn addOn;
    private Subscription subscription;
    private Payment payment;
    private PaymentResponseDTO paymentResponse;

    @BeforeEach
    void setUp() {

        addOn = new AddOn();
        addOn.setId(1L);
        addOn.setName("Nutrition");
        addOn.setActive(true);
        addOn.setUnitPrice(BigDecimal.valueOf(100));

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));

        payment = new Payment();
        payment.setPaymentMethod(PaymentMethod.CARD);

        paymentResponse = new PaymentResponseDTO();
        paymentResponse.setPaymentStatus(PaymentStatus.SUCCESS);
    }

    @Test
    void createAddOnSuccess() {

        when(addOnRepository.findByNameIgnoreCaseAndActiveTrue("Nutrition"))
                .thenReturn(Optional.empty());

        when(addOnRepository.save(addOn))
                .thenReturn(addOn);

        AddOn result = addOnService.createAddOn(addOn);

        assertNotNull(result);
        assertTrue(result.getActive());

        verify(addOnRepository).save(addOn);
    }

    @Test
    void createAddOnDuplicateName() {

        when(addOnRepository.findByNameIgnoreCaseAndActiveTrue("Nutrition"))
                .thenReturn(Optional.of(addOn));

        assertThrows(IllegalArgumentException.class,
                () -> addOnService.createAddOn(addOn));

        verify(addOnRepository, never()).save(any());
    }

    @Test
    void getAllActiveAddOnsSuccess() {

        when(addOnRepository.findByActiveTrue())
                .thenReturn(List.of(addOn));

        List<AddOn> result = addOnService.getAllActiveAddOns();

        assertEquals(1, result.size());

        verify(addOnRepository).findByActiveTrue();
    }

    @Test
    void attachNewAddOnSuccess() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        when(subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription, addOn))
                .thenReturn(Optional.empty());

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(paymentService.processPayment(
                anyLong(),
                any(),
                any(),
                eq(PaymentType.ADDON)))
                .thenReturn(paymentResponse);

        when(subscriptionAddOnRepository.save(any(SubscriptionAddOn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionAddOnResponseDTO result =
                addOnService.attachAddOn(1L,1L,4);

        assertEquals(4, result.getUnitsIncluded());

        verify(subscriptionAddOnRepository).save(any());
    }

    @Test
    void attachExistingAddOnSuccess() {

        SubscriptionAddOn existing = new SubscriptionAddOn();
        existing.setAddOn(addOn);
        existing.setUnitsIncluded(4);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        when(subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription, addOn))
                .thenReturn(Optional.of(existing));

        when(paymentRepository.findTopBySubscriptionOrderByPaymentDateDesc(subscription))
                .thenReturn(Optional.of(payment));

        when(paymentService.processPayment(anyLong(), any(), any(), any()))
                .thenReturn(paymentResponse);

        when(subscriptionAddOnRepository.save(any(SubscriptionAddOn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionAddOnResponseDTO result =
                addOnService.attachAddOn(1L,1L,2);

        assertEquals(6,result.getUnitsIncluded());
    }

    @Test
    void recordUsageSuccess() {

        SubscriptionAddOn subAddOn = new SubscriptionAddOn();
        subAddOn.setAddOn(addOn);
        subAddOn.setUnitsIncluded(10);
        subAddOn.setUnitsUsed(2);

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        when(subscriptionAddOnRepository.findBySubscriptionAndAddOn(subscription, addOn))
                .thenReturn(Optional.of(subAddOn));

        when(subscriptionAddOnRepository.save(any(SubscriptionAddOn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionAddOnResponseDTO result =
                addOnService.recordUsage(1L,1L,3);

        assertEquals(5,result.getUnitsUsed());

        verify(subscriptionAddOnRepository).save(subAddOn);
    }

    @Test
    void calculateAddOnPriceSuccess() {

        AddOnRequestDTO dto = new AddOnRequestDTO();
        dto.setAddOnId(1L);
        dto.setUnitsIncluded(3);

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        BigDecimal result =
                addOnService.calculateAddOnPrice(List.of(dto));

        assertEquals(BigDecimal.valueOf(300),result);
    }

    @Test
    void calculateAddOnPriceEmptyList() {

        BigDecimal result =
                addOnService.calculateAddOnPrice(List.of());

        assertEquals(BigDecimal.ZERO,result);
    }

    @Test
    void getAddOnsForSubscription_ShouldReturnMappedList() {

        SubscriptionAddOn subAddOn = new SubscriptionAddOn();
        subAddOn.setAddOn(addOn);
        subAddOn.setUnitsIncluded(4);
        subAddOn.setUnitsUsed(2);
        subAddOn.setBillingCycleStart(LocalDate.now());
        subAddOn.setBillingCycleEnd(LocalDate.now().plusDays(30));

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of(subAddOn));

        var result = addOnService.getAddOnsForSubscription(1L);

        assertEquals(1, result.size());
        assertEquals("Nutrition", result.get(0).getAddOnName());
        assertEquals(4, result.get(0).getUnitsIncluded());
        assertEquals(2, result.get(0).getUnitsUsed());
    }

    @Test
    void getAddOnsForSubscription_ShouldThrow_WhenSubscriptionNotFound() {

        when(subscriptionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> addOnService.getAddOnsForSubscription(99L));
    }

    @Test
    void carryForwardOnUpgrade_ShouldKeepOnlyUnusedUnits_AndResetUsage() {

        // 4 sessions bought, 3 used -> only 1 unused unit should carry forward for free
        SubscriptionAddOn existing = new SubscriptionAddOn();
        existing.setAddOn(addOn);
        existing.setUnitsIncluded(4);
        existing.setUnitsUsed(3);
        existing.setBillingCycleStart(LocalDate.now().minusDays(10));
        existing.setBillingCycleEnd(LocalDate.now().plusDays(20));

        // subscription dates already moved to the new upgraded cycle
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of(existing));

        addOnService.carryForwardAndMergeAddOnsOnUpgrade(subscription, null);

        assertEquals(1, existing.getUnitsIncluded());
        assertEquals(0, existing.getUnitsUsed());
        assertEquals(subscription.getStartDate(), existing.getBillingCycleStart());
        assertEquals(subscription.getEndDate(), existing.getBillingCycleEnd());

        verify(subscriptionAddOnRepository).saveAll(List.of(existing));
        verify(addOnRepository, never()).findById(any());
    }

    @Test
    void carryForwardOnUpgrade_ShouldMergeNewlyRequestedUnits_OnTopOfCarriedForwardUnits() {

        // 1 unused unit carries forward, then 5 more are bought during the upgrade -> 6 total
        SubscriptionAddOn existing = new SubscriptionAddOn();
        existing.setAddOn(addOn);
        existing.setUnitsIncluded(4);
        existing.setUnitsUsed(3);

        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));

        AddOnRequestDTO newRequest = new AddOnRequestDTO();
        newRequest.setAddOnId(1L);
        newRequest.setUnitsIncluded(5);

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of(existing));

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        addOnService.carryForwardAndMergeAddOnsOnUpgrade(subscription, List.of(newRequest));

        assertEquals(6, existing.getUnitsIncluded());
        assertEquals(0, existing.getUnitsUsed());

        verify(subscriptionAddOnRepository).save(existing);
    }

    @Test
    void carryForwardOnUpgrade_ShouldCreateNewRow_WhenAddOnWasNotPreviouslyAttached() {

        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));

        AddOnRequestDTO newRequest = new AddOnRequestDTO();
        newRequest.setAddOnId(1L);
        newRequest.setUnitsIncluded(2);

        when(subscriptionAddOnRepository.findBySubscription(subscription))
                .thenReturn(List.of());

        when(addOnRepository.findById(1L))
                .thenReturn(Optional.of(addOn));

        when(subscriptionAddOnRepository.save(any(SubscriptionAddOn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        addOnService.carryForwardAndMergeAddOnsOnUpgrade(subscription, List.of(newRequest));

        verify(subscriptionAddOnRepository).save(argThat(saved ->
                saved.getUnitsIncluded() == 2 && saved.getUnitsUsed() == 0));
    }
}
