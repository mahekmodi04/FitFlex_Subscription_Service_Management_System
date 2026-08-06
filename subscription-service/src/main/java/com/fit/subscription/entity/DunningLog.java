package com.fit.subscription.entity;

import com.fit.subscription.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="dunning_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DunningLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = true, length = 255)
    private String failureReason;

    @Column(nullable = true)
    private LocalDate nextRetryDate;

}
