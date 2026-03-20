package com.capstone.paymentservice.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SePayIPNRequest {
    private Long timestamp;
    private String notificationType;
    private OrderDto order;
    private TransactionDto transaction;
    private CustomerDto customer;

    @Data
    public static class OrderDto {
        private String id;
        private String orderId;
        private String orderStatus;
        private String orderCurrency;
        private String orderAmount;
        private String orderInvoiceNumber;
        private List<Object> customData;
        private String userAgent;
        private String ipAddress;
        private String orderDescription;
    }

    @Data
    public static class TransactionDto {
        private String id;
        private String paymentMethod;
        private String transactionId;
        private String transactionType;
        private String transactionDate;
        private String transactionStatus;
        private String transactionAmount;
        private String transactionCurrency;
        private String authenticationStatus;
        private String cardNumber;
        private String cardHolderName;
        private String cardExpiry;
        private String cardFundingMethod;
        private String cardBrand;
    }

    @Data
    public static class CustomerDto {
        private String id;
        private String customerId;
    }
}