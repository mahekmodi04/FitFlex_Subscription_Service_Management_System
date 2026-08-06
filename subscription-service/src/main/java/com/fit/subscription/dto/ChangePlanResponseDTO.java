package com.fit.subscription.dto;

import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePlanResponseDTO {

    private String userName;

    private Long subscriptionId;

    private String newPlanName;

    private BigDecimal newFinalPrice;

    private LocalDate newStartDate;

    private LocalDate newEndDate;

    private Boolean autoRenew = true;

    private SubscriptionStatus status;

    // reflects whether the upgrade payment succeeded - if FAILED, status will be PENDING
    // and none of the plan/date/add-on fields above will have actually changed
    private PaymentStatus paymentStatus;
}
