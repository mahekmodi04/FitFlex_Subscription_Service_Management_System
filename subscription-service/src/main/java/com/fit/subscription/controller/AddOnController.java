package com.fit.subscription.controller;

import com.fit.subscription.dto.SubscriptionAddOnResponseDTO;
import com.fit.subscription.entity.AddOn;
import com.fit.subscription.service.AddOnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addons")

public class AddOnController {
    private final AddOnService addOnService;

    public AddOnController(AddOnService addOnService){
        this.addOnService = addOnService;
    }

    //create a new addon (admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AddOn createAddOn(@Valid @RequestBody AddOn addOn){
        return addOnService.createAddOn(addOn);
    }

    //get all active addons
    @GetMapping
    public List<AddOn> getAllActiveAddOns(){
        return addOnService.getAllActiveAddOns();
    }

    //attach addon to an active subscription - only the owner of that subscription (or an admin) may attach
    @PostMapping("/attach")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#subscriptionId)")
    public SubscriptionAddOnResponseDTO attachAddOn(
            @RequestParam Long subscriptionId,
            @RequestParam Long addOnId,
            @RequestParam Integer unitsIncluded
    ){
        return addOnService.attachAddOn(subscriptionId, addOnId, unitsIncluded);
    }

    //record usage of an AddOn - only the owner of that subscription (or an admin) may record usage
    @PostMapping("/usage")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#subscriptionId)")
    public SubscriptionAddOnResponseDTO recordUsage(
            @RequestParam Long subscriptionId,
            @RequestParam Long addOnId,
            @RequestParam Integer units
    ){
        return addOnService.recordUsage(subscriptionId, addOnId, units);
    }

    // list all add-ons attached to a subscription, with usage - only the owner (or an admin)
    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#subscriptionId)")
    public List<SubscriptionAddOnResponseDTO> getAddOnsForSubscription(@PathVariable Long subscriptionId){
        return addOnService.getAddOnsForSubscription(subscriptionId);
    }
}
