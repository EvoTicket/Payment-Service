package com.capstone.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutTransaction;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PayoutResult {
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "reference_id")
    private String referenceId;

    @OneToMany(
            mappedBy = "payoutResult",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PayoutTransactionResult> transactions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> category;

    private PayoutApprovalState approvalState;

    private String createdAt;
}
