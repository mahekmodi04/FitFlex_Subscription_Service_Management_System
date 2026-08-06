package com.fit.subscription.entity;

import com.fit.subscription.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name="subscriptions")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    private Boolean autoRenew = true;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal finalPrice; //after coupon discount

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id" , nullable = true)
    private Coupon coupon;

    private Integer renewalAttempts = 0;

    @Column(nullable = true)
    private LocalDate nextRetryDate;

    @Column(nullable = true)
    private LocalDate graceEndDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
