package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.dto.ChangePlanRequestDTO;
import com.fit.subscription.dto.ChangePlanResponseDTO;
import com.fit.subscription.dto.CreateSubscriptionRequest;
import com.fit.subscription.dto.SubscriptionResponseDTO;
import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.SubscriptionStatus;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.service.SubscriptionService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private CreateSubscriptionRequest createRequest() {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();

        request.setUserId(1L);
        request.setPlanId(1L);
        request.setCouponCode("WELCOME50");
        request.setAutoRenew(true);
        request.setPaymentMethod(PaymentMethod.CARD);

        return request;
    }
    private SubscriptionResponseDTO createResponse() {

        SubscriptionResponseDTO response = new SubscriptionResponseDTO();

        response.setId(1L);
        response.setUserName("Mahek");
        response.setPlanName("Premium");
        response.setCouponCode("WELCOME50");
        response.setFinalPrice(BigDecimal.valueOf(899));
        response.setAutoRenew(true);
        response.setStartDate(LocalDate.now());
        response.setEndDate(LocalDate.now().plusDays(30));
        response.setStatus(SubscriptionStatus.ACTIVE);
        response.setPaymentStatus(PaymentStatus.SUCCESS);

        return response;
    }
    private ChangePlanRequestDTO createChangePlanRequest() {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();

        request.setSubscriptionId(1L);
        request.setNewPlanId(2L);
        request.setPaymentMethod(PaymentMethod.CARD);

        return request;
    }
    private ChangePlanResponseDTO createChangePlanResponse() {

        ChangePlanResponseDTO response = new ChangePlanResponseDTO();

        response.setSubscriptionId(1L);
        response.setUserName("Mahek");
        response.setNewPlanName("Enterprise");
        response.setNewFinalPrice(BigDecimal.valueOf(1499));
        response.setNewStartDate(LocalDate.now());
        response.setNewEndDate(LocalDate.now().plusDays(30));
        response.setAutoRenew(true);
        response.setStatus(SubscriptionStatus.ACTIVE);

        return response;
    }
    @Test
    @DisplayName("Should create subscription successfully")
    void createSubscription_ShouldReturnResponse() throws Exception {

        CreateSubscriptionRequest request = createRequest();

        SubscriptionResponseDTO response = createResponse();

        when(subscriptionService.createSubscription(any(CreateSubscriptionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Mahek"))
                .andExpect(jsonPath("$.planName").value("Premium"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        verify(subscriptionService, times(1))
                .createSubscription(any(CreateSubscriptionRequest.class));
    }
    @Test
    @DisplayName("Should change plan successfully")
    void changePlan_ShouldReturnResponse() throws Exception {

        ChangePlanRequestDTO request = createChangePlanRequest();

        ChangePlanResponseDTO response = createChangePlanResponse();

        when(subscriptionService.changePlan(any(ChangePlanRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/subscriptions/change-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newPlanName").value("Enterprise"))
                .andExpect(jsonPath("$.newFinalPrice").value(1499));

        verify(subscriptionService, times(1))
                .changePlan(any(ChangePlanRequestDTO.class));
    }
    @Test
    @DisplayName("Should cancel subscription successfully")
    void cancelSubscription_ShouldReturnResponse() throws Exception {

        SubscriptionResponseDTO response = createResponse();

        response.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionService.cancelSubscription(1L))
                .thenReturn(response);

        mockMvc.perform(put("/subscriptions/{subscriptionId}/cancel", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(subscriptionService, times(1))
                .cancelSubscription(1L);
    }
    @Test
    @DisplayName("Should return BadRequest when create subscription request is invalid")
    void createSubscription_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return BadRequest when change plan request is invalid")
    void changePlan_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        ChangePlanRequestDTO request = new ChangePlanRequestDTO();

        mockMvc.perform(put("/subscriptions/change-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return NotFound when subscription does not exist")
    void cancelSubscription_ShouldReturnNotFound() throws Exception {

        when(subscriptionService.cancelSubscription(100L))
                .thenThrow(new ResourceNotFoundException("Subscription not found"));

        mockMvc.perform(put("/subscriptions/{subscriptionId}/cancel", 100L))
                .andExpect(status().isNotFound());

        verify(subscriptionService, times(1))
                .cancelSubscription(100L);
    }
}
