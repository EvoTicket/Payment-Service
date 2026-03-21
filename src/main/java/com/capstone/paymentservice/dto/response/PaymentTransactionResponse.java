package com.capstone.paymentservice.dto.response;

import com.capstone.paymentservice.entity.PaymentTransaction;
import com.capstone.paymentservice.enums.EventPublishStatus;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.enums.PaymentStatus;
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
public class PaymentTransactionResponse {
    private Long id;
    private Long orderCode;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String buyerName;
    private String buyerEmail;
    private String description;
    private String transactionDateTime;
    private String paymentLinkUrl;
    private EventPublishStatus eventPublishStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentTransactionResponse fromEntity(PaymentTransaction entity) {
        return PaymentTransactionResponse.builder()
                .id(entity.getId())
                .orderCode(entity.getOrderCode())
                .transactionId(entity.getTransactionId())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .amount(entity.getAmount())
                .buyerName(entity.getBuyerName())
                .buyerEmail(entity.getBuyerEmail())
                .description(entity.getDescription())
                .transactionDateTime(entity.getTransactionDateTime())
                .paymentLinkUrl(entity.getPaymentLinkUrl())
                .eventPublishStatus(entity.getEventPublishStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
