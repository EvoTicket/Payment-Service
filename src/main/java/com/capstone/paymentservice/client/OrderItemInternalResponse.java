package com.capstone.paymentservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemInternalResponse {
    private Long id;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String ticketCode;
    private String tokenId;
    private LocalDateTime createdAt;
}