package com.capstone.paymentservice.dto.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private String orderCode;

    private String transactionDateTime;

    private String transactionId;
}
