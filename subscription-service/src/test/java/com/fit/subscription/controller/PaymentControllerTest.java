package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.service.PaymentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentResponseDTO createPaymentResponse() {

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setPaymentId(1L);
        response.setSubscriptionId(1L);
        response.setAmount(BigDecimal.valueOf(999));
        response.setPaymentMethod(PaymentMethod.CARD);
        response.setPaymentStatus(PaymentStatus.SUCCESS);
        response.setTransactionId("TXN123");
        response.setPaymentDate(LocalDateTime.now());
        response.setPaymentType(PaymentType.SUBSCRIPTION);

        return response;
    }
    @Test
    @DisplayName("Should return payment by id")
    void getPaymentById_ShouldReturnPayment() throws Exception {

        PaymentResponseDTO response = createPaymentResponse();

        when(paymentService.getPaymentById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/payments/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.transactionId").value("TXN123"))
                .andExpect(jsonPath("$.amount").value(999));

        verify(paymentService, times(1))
                .getPaymentById(1L);
    }
    @Test
    @DisplayName("Should return all payments")
    void getAllPayments_ShouldReturnPayments() throws Exception {

        PaymentResponseDTO response1 = createPaymentResponse();

        PaymentResponseDTO response2 = createPaymentResponse();
        response2.setPaymentId(2L);
        response2.setTransactionId("TXN456");

        when(paymentService.getAllPayments())
                .thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN123"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN456"));

        verify(paymentService, times(1))
                .getAllPayments();
    }
    @Test
    @DisplayName("Should return payments of subscription")
    void getPaymentsBySubscription_ShouldReturnPayments() throws Exception {

        PaymentResponseDTO response = createPaymentResponse();

        when(paymentService.getPaymentsBySubscription(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/payments/subscription/{subscriptionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN123"));

        verify(paymentService, times(1))
                .getPaymentsBySubscription(1L);
    }
    @Test
    @DisplayName("Should return 404 when payment not found")
    void getPaymentById_ShouldReturnNotFound() throws Exception {

        when(paymentService.getPaymentById(100L))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/payments/{id}", 100L))
                .andExpect(status().isNotFound());

        verify(paymentService, times(1))
                .getPaymentById(100L);
    }
}
