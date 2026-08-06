package com.fit.subscription.controller;

import com.fit.subscription.entity.Plan;
import com.fit.subscription.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plans")

public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // Create Plan
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Plan createPlan(@Valid @RequestBody Plan plan) {
        return planService.createPlan(plan);
    }

    // Get Plan By id
    @GetMapping("/{id}")
    public Plan getPlanById(@PathVariable Long id) {
        return planService.getPlanById(id);
    }

    // Get All Plans
    @GetMapping
    public List<Plan> getAllPlans() {
        return planService.getAllPlans();
    }

    // Update Plan
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Plan updatePlan(@PathVariable Long id, @Valid @RequestBody Plan updatedPlan) {
        return planService.updatePlan(id, updatedPlan);
    }

    // Delete Plan
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return "Plan deleted successfully";
    }

}
