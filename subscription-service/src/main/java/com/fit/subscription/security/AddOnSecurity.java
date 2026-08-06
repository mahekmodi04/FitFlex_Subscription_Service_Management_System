package com.fit.subscription.security;

import com.fit.subscription.repository.SubscriptionAddOnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("addOnSecurity")
@RequiredArgsConstructor
public class AddOnSecurity {

    private final SubscriptionAddOnRepository subscriptionAddOnRepository;

    public boolean isOwner(Long subscriptionAddOnId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return subscriptionAddOnRepository.findById(subscriptionAddOnId)

                .map(subscriptionAddOn ->
                        subscriptionAddOn
                                .getSubscription()
                                .getUser()
                                .getId()
                                .equals(principal.getUser().getId()))

                .orElse(false);
    }
}
