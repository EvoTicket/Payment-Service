package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.PaymentTransactionResponse;
import com.capstone.paymentservice.service.PaymentTransactionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutRequests;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentTransactionService paymentTransactionService;
    private final PayOS payOSPayout;

    public PaymentController(
            PaymentTransactionService paymentTransactionService,
            @Qualifier("payOSPayout") PayOS payOSPayout
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.payOSPayout = payOSPayout;
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<BaseResponse<PaymentTransactionResponse>> getPaymentInfo(
            @PathVariable String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Lấy trạng thái thanh toán thành công", paymentTransactionService.getTransactionByOrderCode(orderCode)));
    }

    @PostMapping("/create")
    public BaseResponse<Payout> create(@RequestBody PayoutRequests body) {
        try {
            if (body.getReferenceId() == null || body.getReferenceId().isEmpty()) {
                body.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
            }

            Payout payout = payOSPayout.payouts().create(body);
            return BaseResponse.ok("ok", payout);

        } catch (Exception e) {
            e.printStackTrace();
            return BaseResponse.badRequest("fail");
        }
    }

}