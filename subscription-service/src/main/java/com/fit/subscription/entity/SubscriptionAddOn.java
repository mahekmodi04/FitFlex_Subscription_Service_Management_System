package com.fit.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name="subscription_addons")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class SubscriptionAddOn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_id", nullable = false)
    private AddOn addOn;

    @Column(nullable = false)
    private Integer unitsUsed = 0;

    @Column(nullable = false)
    private Integer unitsIncluded = 0;

    @Column(nullable = false)
    private LocalDate billingCycleStart;

    @Column(nullable = false)
    private LocalDate billingCycleEnd;
}
