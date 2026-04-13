package com.capstone.paymentservice.client;

import com.capstone.paymentservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInternalResponse {
    private Long id;
    private PaymentMethod paymentMethod;
    private String orderCode;
    private String buyerName;
    private String buyerPhone;
    private String buyerEmail;
    private BigDecimal finalAmount;
    private List<OrderItemInternalResponse> items;
}
