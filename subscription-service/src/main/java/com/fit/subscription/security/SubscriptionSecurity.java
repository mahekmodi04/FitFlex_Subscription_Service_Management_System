package com.fit.subscription.security;

import com.fit.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("subscriptionSecurity")
@RequiredArgsConstructor
public class SubscriptionSecurity {

    private final SubscriptionRepository subscriptionRepository;

    public boolean isOwner(Long subscriptionId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        return subscriptionRepository.findById(subscriptionId)

                .map(subscription ->
                        subscription.getUser().getId()
                                .equals(principal.getUser().getId()))

                .orElse(false);
    }

}
