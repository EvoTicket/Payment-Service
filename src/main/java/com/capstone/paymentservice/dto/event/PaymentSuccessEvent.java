package com.capstone.paymentservice.dto.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private Long orderCode;

    private BigDecimal amount;

    private String description;

    private String transactionDateTime;

    private String transactionId;
}
