package com.fit.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.subscription.entity.Plan;
import com.fit.subscription.enums.PlanType;
import com.fit.subscription.exception.ResourceNotFoundException;
import com.fit.subscription.service.PlanService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanService planService;

    @Test
    @DisplayName("Should create plan successfully")
    void createPlan_ShouldReturnCreatedPlan() throws Exception {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setPrice(BigDecimal.valueOf(999));
        plan.setDurationDays(30);
        plan.setDescription("Premium Subscription");
        plan.setTier(PlanType.BASIC);
        plan.setActive(true);

        when(planService.createPlan(any(Plan.class)))
                .thenReturn(plan);

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Premium"))
                .andExpect(jsonPath("$.price").value(999));

        verify(planService, times(1))
                .createPlan(any(Plan.class));
    }
    @Test
    @DisplayName("Should return plan by id")
    void getPlanById_ShouldReturnPlan() throws Exception {

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium");
        plan.setPrice(BigDecimal.valueOf(999));
        plan.setDurationDays(30);
        plan.setDescription("Premium Subscription");
        plan.setTier(PlanType.BASIC);
        plan.setActive(true);

        when(planService.getPlanById(1L))
                .thenReturn(plan);

        mockMvc.perform(get("/plans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Premium"))
                .andExpect(jsonPath("$.price").value(999));

        verify(planService, times(1))
                .getPlanById(1L);
    }
    @Test
    @DisplayName("Should return all plans")
    void getAllPlans_ShouldReturnPlans() throws Exception {

        Plan plan1 = new Plan();
        plan1.setId(1L);
        plan1.setName("Basic");
        plan1.setPrice(BigDecimal.valueOf(499));
        plan1.setDurationDays(30);
        plan1.setTier(PlanType.BASIC);
        plan1.setActive(true);

        Plan plan2 = new Plan();
        plan2.setId(2L);
        plan2.setName("Premium");
        plan2.setPrice(BigDecimal.valueOf(999));
        plan2.setDurationDays(30);
        plan2.setTier(PlanType.BASIC);
        plan2.setActive(true);

        when(planService.getAllPlans())
                .thenReturn(List.of(plan1, plan2));

        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Basic"))
                .andExpect(jsonPath("$[1].name").value("Premium"));

        verify(planService, times(1))
                .getAllPlans();
    }
    @Test
    @DisplayName("Should update plan successfully")
    void updatePlan_ShouldReturnUpdatedPlan() throws Exception {

        Plan updatedPlan = new Plan();
        updatedPlan.setId(1L);
        updatedPlan.setName("Updated Premium");
        updatedPlan.setPrice(BigDecimal.valueOf(1299));
        updatedPlan.setDurationDays(60);
        updatedPlan.setDescription("Updated Plan");
        updatedPlan.setTier(PlanType.BASIC);
        updatedPlan.setActive(true);

        when(planService.updatePlan(any(Long.class), any(Plan.class)))
                .thenReturn(updatedPlan);

        mockMvc.perform(put("/plans/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPlan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Premium"))
                .andExpect(jsonPath("$.price").value(1299));

        verify(planService, times(1))
                .updatePlan(any(Long.class), any(Plan.class));
    }
    @Test
    @DisplayName("Should delete plan successfully")
    void deletePlan_ShouldReturnSuccessMessage() throws Exception {

        doNothing().when(planService).deletePlan(1L);

        mockMvc.perform(delete("/plans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().string("Plan deleted successfully"));

        verify(planService, times(1))
                .deletePlan(1L);
    }
    @Test
    @DisplayName("Should return BadRequest when validation fails")
    void createPlan_ShouldReturnBadRequest_WhenValidationFails() throws Exception {

        Plan plan = new Plan();

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(plan)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("Should return 404 when plan not found")
    void getPlanById_ShouldReturnNotFound() throws Exception {

        when(planService.getPlanById(100L))
                .thenThrow(new ResourceNotFoundException("Plan not found"));

        mockMvc.perform(get("/plans/{id}", 100L))
                .andExpect(status().isNotFound());

        verify(planService, times(1))
                .getPlanById(100L);
    }


}
