package com.capstone.paymentservice.dto.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private Long orderCode;

    private Long amount;

    private String description;

    private String transactionDateTime;

    private String transactionId;
}
