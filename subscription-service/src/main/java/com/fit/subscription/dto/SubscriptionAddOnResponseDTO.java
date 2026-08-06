package com.fit.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionAddOnResponseDTO {

    private Long addOnId;

    private String addOnName;

    private Integer unitsIncluded;

    private Integer unitsUsed;

    private BigDecimal unitPrice;

    private LocalDate billingCycleStart;

    private LocalDate billingCycleEnd;
}
