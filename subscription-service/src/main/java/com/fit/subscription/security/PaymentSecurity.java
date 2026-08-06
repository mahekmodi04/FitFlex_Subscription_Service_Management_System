package com.fit.subscription.security;

import com.fit.subscription.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentRepository paymentRepository;

    public boolean isOwner(Long paymentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return paymentRepository.findById(paymentId)

                .map(payment ->
                        payment.getSubscription()
                                .getUser()
                                .getId()
                                .equals(principal.getUser().getId()))

                .orElse(false);
    }

}
