package com.fit.subscription.dto;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.entity.Plan;
import com.fit.subscription.entity.User;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SubscriptionResponseDTO {
    private Long id;

    private String userName;

    private String planName;

    private String couponCode;

    private BigDecimal finalPrice;

    private Boolean autoRenew = true;

    private LocalDate startDate;

    private LocalDate endDate;

    private SubscriptionStatus status;

    private PaymentStatus paymentStatus;

    private LocalDate nextRetryDate;

    private LocalDate graceEndDate;
}
