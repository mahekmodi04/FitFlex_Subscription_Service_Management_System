package com.fit.subscription.repository;

import com.fit.subscription.entity.Payment;
import com.fit.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopBySubscriptionOrderByPaymentDateDesc(Subscription subscription);
    List<Payment> findBySubscriptionId(Long subscriptionId);
}
