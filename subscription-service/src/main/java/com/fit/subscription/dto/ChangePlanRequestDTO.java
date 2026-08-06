package com.fit.subscription.dto;

import com.fit.subscription.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePlanRequestDTO {

    @NotNull
    private Long subscriptionId;

    @NotNull
    private Long newPlanId;

    @NotNull
    private PaymentMethod paymentMethod;

    // any NEW add-ons the user wants to buy at the moment of upgrade (in addition to
    // whatever unused add-on units already carry forward for free from the old plan)
    private List<@Valid AddOnRequestDTO> addOns;

    // if true, redeem as much of the user's wallet balance as possible against this charge
    private Boolean useWalletBalance = false;
}
