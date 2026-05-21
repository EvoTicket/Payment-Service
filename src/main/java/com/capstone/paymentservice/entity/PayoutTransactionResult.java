package com.capstone.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.payos.model.v1.payouts.PayoutTransactionState;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PayoutTransactionResult {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_result_id")
    private PayoutResult payoutResult;

    @Id
    private String id;

    private String referenceId;

    private Long amount;

    private String description;

    private String toBin;

    private String toAccountNumber;

    private String toAccountName;

    private String reference;

    private String transactionDatetime;

    private String errorMessage;

    private String errorCode;

    private PayoutTransactionState state;

}
