package com.fit.subscription.entity;

import com.fit.subscription.enums.CouponType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="coupons")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = true, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @Column(nullable = false)
    private Integer usageLimit;

    private Integer usedCount = 0;

    private Boolean active = true;

    @NotNull
    @Future
    @Column(nullable = false)
    private LocalDate expiryDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;
}
