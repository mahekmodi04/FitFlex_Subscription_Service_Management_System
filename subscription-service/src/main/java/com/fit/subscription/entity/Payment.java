package com.fit.subscription.entity;

import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payments")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 10,scale = 2)
    private BigDecimal amount;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(unique = true, length = 100)
    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = true, precision = 10,scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = true, length = 255)
    private String refundReason;

    @Column(nullable = true)
    private LocalDateTime refundDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;


}
