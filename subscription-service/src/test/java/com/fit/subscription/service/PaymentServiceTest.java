package com.fit.subscription.service;

import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.repository.PaymentRepository;
import com.fit.subscription.repository.SubscriptionRepository;
import com.fit.subscription.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentGatewaySimulator paymentGatewaySimulator;

    @InjectMocks
    private PaymentService paymentService;

    private Subscription subscription;

    @BeforeEach
    void setUp() {

        User user = new User();
        user.setId(1L);

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUser(user);
    }

    @Test
    void processPayment_ShouldReturnSuccessResponse() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(paymentGatewaySimulator.gatewayStatus(anyString()))
                .thenReturn(true);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {

                    Payment payment = invocation.getArgument(0);
                    payment.setId(100L);
                    return payment;
                });

        PaymentResponseDTO response = paymentService.processPayment(
                1L,
                BigDecimal.valueOf(999),
                PaymentMethod.CARD,
                PaymentType.SUBSCRIPTION
        );

        assertNotNull(response);
        assertEquals(100L, response.getPaymentId());
        assertEquals(1L, response.getSubscriptionId());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
        assertEquals(PaymentType.SUBSCRIPTION, response.getPaymentType());
        assertEquals(BigDecimal.valueOf(999), response.getAmount());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_ShouldReturnFailedResponse_WhenGatewayFails() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(paymentGatewaySimulator.gatewayStatus(anyString()))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {

                    Payment payment = invocation.getArgument(0);
                    payment.setId(200L);
                    return payment;
                });

        PaymentResponseDTO response = paymentService.processPayment(
                1L,
                BigDecimal.valueOf(500),
                PaymentMethod.UPI,
                PaymentType.ADDON
        );

        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        assertEquals(PaymentType.ADDON, response.getPaymentType());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void processPayment_ShouldThrowException_WhenSubscriptionNotFound() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processPayment(
                        1L,
                        BigDecimal.valueOf(500),
                        PaymentMethod.UPI,
                        PaymentType.SUBSCRIPTION
                ));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_ShouldThrowException_WhenAmountIsZero() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment(
                        1L,
                        BigDecimal.ZERO,
                        PaymentMethod.UPI,
                        PaymentType.SUBSCRIPTION
                ));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_ShouldThrowException_WhenAmountIsNegative() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment(
                        1L,
                        BigDecimal.valueOf(-500),
                        PaymentMethod.UPI,
                        PaymentType.SUBSCRIPTION
                ));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_ShouldSaveCorrectPaymentObject() {

        when(subscriptionRepository.findById(1L))
                .thenReturn(Optional.of(subscription));

        when(paymentGatewaySimulator.gatewayStatus(anyString()))
                .thenReturn(true);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPayment(
                1L,
                BigDecimal.valueOf(1200),
                PaymentMethod.CARD,
                PaymentType.RENEWAL
        );

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(captor.capture());

        Payment savedPayment = captor.getValue();

        assertEquals(BigDecimal.valueOf(1200), savedPayment.getAmount());
        assertEquals(PaymentMethod.CARD, savedPayment.getPaymentMethod());
        assertEquals(PaymentType.RENEWAL, savedPayment.getPaymentType());
        assertEquals(subscription, savedPayment.getSubscription());
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getPaymentStatus());
        assertNotNull(savedPayment.getTransactionId());
        assertNotNull(savedPayment.getPaymentDate());
    }

    @Test
    void getPaymentById_ShouldReturnResponse() {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSubscription(subscription);
        payment.setAmount(BigDecimal.valueOf(999));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("TXN123");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentType(PaymentType.SUBSCRIPTION);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L);

        assertEquals(1L, response.getPaymentId());
        assertEquals("TXN123", response.getTransactionId());
    }

    @Test
    void getPaymentById_ShouldThrow_WhenNotFound() {

        when(paymentRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(100L));
    }

    @Test
    void getAllPayments_ShouldReturnAll() {

        Payment payment1 = new Payment();
        payment1.setId(1L);
        payment1.setSubscription(subscription);
        payment1.setPaymentMethod(PaymentMethod.CARD);
        payment1.setPaymentStatus(PaymentStatus.SUCCESS);
        payment1.setPaymentType(PaymentType.SUBSCRIPTION);

        Payment payment2 = new Payment();
        payment2.setId(2L);
        payment2.setSubscription(subscription);
        payment2.setPaymentMethod(PaymentMethod.UPI);
        payment2.setPaymentStatus(PaymentStatus.SUCCESS);
        payment2.setPaymentType(PaymentType.RENEWAL);

        when(paymentRepository.findAll())
                .thenReturn(List.of(payment1, payment2));

        List<PaymentResponseDTO> result = paymentService.getAllPayments();

        assertEquals(2, result.size());
    }

    @Test
    void getPaymentsBySubscription_ShouldReturnMatching() {

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSubscription(subscription);
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentType(PaymentType.SUBSCRIPTION);

        when(paymentRepository.findBySubscriptionId(1L))
                .thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getPaymentsBySubscription(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSubscriptionId());
    }
}
