package com.capstone.paymentservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemInternalResponse {
    private String ticketTypeName;
    private Long quantity;
    private BigDecimal unitPrice;
}