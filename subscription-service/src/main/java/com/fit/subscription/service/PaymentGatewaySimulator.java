package com.fit.subscription.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentGatewaySimulator {
    public boolean gatewayStatus(String transactionId) {
        if (transactionId.charAt(transactionId.length() - 1) % 2 == 0) {
            return true;
        }
        return false;
    }
}
