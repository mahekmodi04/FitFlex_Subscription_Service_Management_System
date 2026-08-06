package com.fit.subscription.repository;

import com.fit.subscription.entity.Subscription;
import com.fit.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // <= instead of exact equality: if a scheduler run is ever missed (downtime, restart),
    // the subscription is still picked up on the next run instead of being skipped forever.
    List<Subscription> findByEndDateLessThanEqualAndAutoRenewAndStatus(
            LocalDate endDate,
            Boolean autoRenew,
            SubscriptionStatus status
    );

    List<Subscription> findByNextRetryDateLessThanEqualAndStatus(
            LocalDate nextRetryDate,
            SubscriptionStatus status
    );

    List<Subscription> findByUserId(Long userId);
}
