package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.dto.SubscriptionAddOnResponseDTO;
import com.fit.subscription.entity.AddOn;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.SubscriptionAddOn;
import com.fit.subscription.service.AddOnService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AddOnController.class)
class AddOnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddOnService addOnService;
    private AddOn createAddOn() {

        AddOn addOn = new AddOn();
        addOn.setId(1L);
        addOn.setName("Extra Storage");
        addOn.setDescription("100GB Extra Storage");
        addOn.setUnitName("GB");
        addOn.setUnitPrice(BigDecimal.valueOf(200));
        addOn.setActive(true);

        return addOn;
    }
    private SubscriptionAddOn createSubscriptionAddOn() {

        Subscription subscription = new Subscription();
        subscription.setId(1L);

        SubscriptionAddOn subscriptionAddOn = new SubscriptionAddOn();

        subscriptionAddOn.setId(1L);
        subscriptionAddOn.setSubscription(subscription);
        subscriptionAddOn.setAddOn(createAddOn());
        subscriptionAddOn.setUnitsIncluded(5);
        subscriptionAddOn.setUnitsUsed(2);
        subscriptionAddOn.setBillingCycleStart(LocalDate.now());
        subscriptionAddOn.setBillingCycleEnd(LocalDate.now().plusDays(30));

        return subscriptionAddOn;
    }
    private SubscriptionAddOnResponseDTO createSubscriptionAddOnResponseDTO() {
        SubscriptionAddOn subscriptionAddOn = createSubscriptionAddOn();
        return new SubscriptionAddOnResponseDTO(
                subscriptionAddOn.getAddOn().getId(),
                subscriptionAddOn.getAddOn().getName(),
                subscriptionAddOn.getUnitsIncluded(),
                subscriptionAddOn.getUnitsUsed(),
                subscriptionAddOn.getAddOn().getUnitPrice(),
                subscriptionAddOn.getBillingCycleStart(),
                subscriptionAddOn.getBillingCycleEnd()
        );
    }
    @Test
    @DisplayName("Should create AddOn successfully")
    void createAddOn_ShouldReturnCreatedAddOn() throws Exception {

        AddOn addOn = createAddOn();

        when(addOnService.createAddOn(any(AddOn.class)))
                .thenReturn(addOn);

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Extra Storage"))
                .andExpect(jsonPath("$.unitPrice").value(200));

        verify(addOnService, times(1))
                .createAddOn(any(AddOn.class));
    }
    @Test
    @DisplayName("Should return all active AddOns")
    void getAllActiveAddOns_ShouldReturnAddOns() throws Exception {

        AddOn addOn1 = createAddOn();

        AddOn addOn2 = createAddOn();
        addOn2.setId(2L);
        addOn2.setName("Priority Support");

        when(addOnService.getAllActiveAddOns())
                .thenReturn(List.of(addOn1, addOn2));

        mockMvc.perform(get("/addons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Extra Storage"))
                .andExpect(jsonPath("$[1].name").value("Priority Support"));

        verify(addOnService, times(1))
                .getAllActiveAddOns();
    }
    @Test
    @DisplayName("Should attach AddOn successfully")
    void attachAddOn_ShouldReturnSubscriptionAddOn() throws Exception {

        SubscriptionAddOnResponseDTO subscriptionAddOn = createSubscriptionAddOnResponseDTO();

        when(addOnService.attachAddOn(1L, 1L, 5))
                .thenReturn(subscriptionAddOn);

        mockMvc.perform(post("/addons/attach")
                        .param("subscriptionId", "1")
                        .param("addOnId", "1")
                        .param("unitsIncluded", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsIncluded").value(5));

        verify(addOnService, times(1))
                .attachAddOn(1L, 1L, 5);
    }
    @Test
    @DisplayName("Should record AddOn usage successfully")
    void recordUsage_ShouldReturnUpdatedSubscriptionAddOn() throws Exception {

        SubscriptionAddOnResponseDTO subscriptionAddOn = createSubscriptionAddOnResponseDTO();

        subscriptionAddOn.setUnitsUsed(3);

        when(addOnService.recordUsage(1L, 1L, 1))
                .thenReturn(subscriptionAddOn);

        mockMvc.perform(post("/addons/usage")
                        .param("subscriptionId", "1")
                        .param("addOnId", "1")
                        .param("units", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsUsed").value(3));

        verify(addOnService, times(1))
                .recordUsage(1L, 1L, 1);
    }
    @Test
    @DisplayName("Should return BadRequest when validation fails")
    void createAddOn_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        AddOn addOn = new AddOn();

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOn)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return BadRequest when AddOn already exists")
    void createAddOn_ShouldReturnBadRequest_WhenAlreadyExists() throws Exception {

        AddOn addOn = createAddOn();

        when(addOnService.createAddOn(any(AddOn.class)))
                .thenThrow(new IllegalArgumentException("AddOn already exists"));

        mockMvc.perform(post("/addons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addOn)))
                .andExpect(status().isBadRequest());

        verify(addOnService, times(1))
                .createAddOn(any(AddOn.class));
    }
}
