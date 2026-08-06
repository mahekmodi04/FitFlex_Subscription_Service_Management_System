package com.fit.subscription.entity;
import com.fit.subscription.enums.PlanType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;


@Entity
@Table(name ="plans")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false,length = 50)
    private String name;

    @NotNull
    @Positive
    @Column(nullable = false,precision = 10,scale=2)
    private BigDecimal price;

    @Min(30)
    @Positive
    @NotNull
    @Column(nullable = false)
    private Integer durationDays;

    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length=20)
    private PlanType tier;

    @Column(nullable=false)
    private Boolean active = true;

}
