package com.fit.subscription.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name="add_ons")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddOn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false,length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(nullable = false,length = 30)
    private String unitName;

    @NotNull
    @DecimalMin("0.0")
    @Positive
    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Boolean active = true;
}
