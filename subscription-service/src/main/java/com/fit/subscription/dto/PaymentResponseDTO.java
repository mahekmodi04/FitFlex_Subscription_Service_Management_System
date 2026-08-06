package com.fit.subscription.dto;

import com.fit.subscription.enums.PaymentMethod;
import com.fit.subscription.enums.PaymentStatus;
import com.fit.subscription.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {
    private Long paymentId;

    private Long subscriptionId;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private String transactionId;

    private LocalDateTime paymentDate;

    private PaymentType paymentType;

}
