package com.fit.subscription.repository;

import com.fit.subscription.entity.AddOn;
import com.fit.subscription.entity.Subscription;
import com.fit.subscription.entity.SubscriptionAddOn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionAddOnRepository extends JpaRepository<SubscriptionAddOn, Long> {

    Optional<SubscriptionAddOn> findBySubscriptionAndAddOn(Subscription subscription,
                                                           AddOn addOn);

    List<SubscriptionAddOn> findBySubscription(Subscription subscription);
}
