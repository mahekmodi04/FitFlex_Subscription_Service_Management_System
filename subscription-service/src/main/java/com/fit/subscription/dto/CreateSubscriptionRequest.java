package com.fit.subscription.dto;

import com.fit.subscription.entity.Coupon;
import com.fit.subscription.entity.Plan;
import com.fit.subscription.entity.User;
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
public class CreateSubscriptionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long planId;

    private String couponCode;

    private Boolean autoRenew = true;

    @NotNull
    private PaymentMethod paymentMethod;

    private List<@Valid AddOnRequestDTO> addOns;

    // if true, redeem as much of the user's wallet balance as possible against this charge
    private Boolean useWalletBalance = false;
}
