package com.fit.subscription.controller;

import com.fit.subscription.dto.PaymentResponseDTO;
import com.fit.subscription.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    //get payment by Id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#id)")
    public PaymentResponseDTO getPaymentById(@PathVariable Long id){
        return paymentService.getPaymentById(id);
    }
    // Get All Payments
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentResponseDTO> getAllPayments() {
        return paymentService.getAllPayments();
    }

    //get all payments by subscription
    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.isOwner(#subscriptionId)")
    public List<PaymentResponseDTO> getPaymentsBySubscription(@PathVariable Long subscriptionId){
        return paymentService.getPaymentsBySubscription(subscriptionId);
    }

}
