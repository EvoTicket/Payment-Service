package com.capstone.paymentservice.service;

import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class PayoutService {
    private final PayOS payOSPayout;
    private final PaymentService paymentService;

    public PayoutService(@Qualifier("payOSPayout") PayOS payOSPayout, PaymentService paymentService) {
        this.payOSPayout = payOSPayout;
        this.paymentService = paymentService;
    }

    public boolean createPayout (BigDecimal amount, String binCode, String bankAccountNumber) {
        PayoutAccountInfo accountInfo = payOSPayout.payoutsAccount().balance();
        if(accountInfo != null && Long.parseLong(accountInfo.getBalance()) < amount.longValue()) {
            log.warn("⚠️ Insufficient balance for payout. Available: {}, Required: {}", accountInfo.getBalance(), amount);
            return false;
        }

        String referenceId = "EvoTicket_Payout" + (System.currentTimeMillis() / 1000);
        PayoutRequests payoutRequest = PayoutRequests.builder()
                .referenceId(referenceId)
                .amount(amount.longValue())
                .description("Resale order " + referenceId)
                .category(List.of("resale"))
                .toBin(binCode)
                .toAccountNumber(bankAccountNumber)
                .build();
        Payout payout = payOSPayout.payouts().create(payoutRequest);
        paymentService.savePayoutResult(payout);
        log.info("Payout created with referenceId: {} + with status: {}", referenceId, payout.getApprovalState());
        return PayoutApprovalState.APPROVED.equals(payout.getApprovalState());
    }
}
