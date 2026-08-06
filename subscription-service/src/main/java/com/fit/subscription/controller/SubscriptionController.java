package com.fit.subscription.controller;

import com.fit.subscription.dto.ChangePlanRequestDTO;
import com.fit.subscription.dto.ChangePlanResponseDTO;
import com.fit.subscription.dto.CreateSubscriptionRequest;
import com.fit.subscription.dto.SubscriptionResponseDTO;
import com.fit.subscription.service.RenewalService;
import com.fit.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")

public class SubscriptionController {
    private final SubscriptionService subscriptionService;
    private final RenewalService renewalService;

    public SubscriptionController(SubscriptionService subscriptionService, RenewalService renewalService) {
        this.subscriptionService = subscriptionService;
        this.renewalService = renewalService;
    }
    //create subscription
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public SubscriptionResponseDTO createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {

        return subscriptionService.createSubscription(request);
    }
    // Get a single subscription's details
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#id)")
    public SubscriptionResponseDTO getSubscriptionById(@PathVariable Long id) {
        return subscriptionService.getSubscriptionById(id);
    }
    // List all subscriptions belonging to a user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(#userId)")
    public List<SubscriptionResponseDTO> getSubscriptionsByUser(@PathVariable Long userId) {
        return subscriptionService.getSubscriptionsByUser(userId);
    }
    // List every subscription across all users - for the admin dashboard/table
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SubscriptionResponseDTO> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }
    //upgrade a plan
    @PutMapping("/change-plan")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#request.subscriptionId)")
    public ChangePlanResponseDTO changePlan(
            @Valid @RequestBody ChangePlanRequestDTO request) {

        return subscriptionService.changePlan(request);
    }
    // Cancel Subscription
    @PutMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#subscriptionId)")
    public SubscriptionResponseDTO cancelSubscription(
            @PathVariable Long subscriptionId) {

        return subscriptionService.cancelSubscription(subscriptionId);

    }

    // Admin-only test helpers: exercise the exact same renewSubscription/retryPayment code the
    // midnight schedulers call, without waiting for real time to pass. Fast-forwards
    // endDate/nextRetryDate to today first if needed.
    @PostMapping("/{id}/test-renewal")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDTO testRenewal(@PathVariable Long id) {
        renewalService.simulateCycleEndForTesting(id);
        return subscriptionService.getSubscriptionById(id);
    }

    @PostMapping("/{id}/test-retry")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionResponseDTO testRetry(@PathVariable Long id) {
        renewalService.simulateRetryForTesting(id);
        return subscriptionService.getSubscriptionById(id);
    }

}
