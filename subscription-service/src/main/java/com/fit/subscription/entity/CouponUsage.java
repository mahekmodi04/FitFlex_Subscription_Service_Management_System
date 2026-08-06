package com.fit.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="coupon_usage",
        uniqueConstraints = {
        @UniqueConstraint(
              columnNames = {"user_id" , "coupon_id"}
        )
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id" , nullable = false)
    private Coupon coupon;

    private Integer usageCount = 0;
}
