package com.capstone.paymentservice.client;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.PaymentLinkResponse;
import com.capstone.paymentservice.dto.response.PaymentTransactionResponse;
import com.capstone.paymentservice.service.PaymentService;
import com.capstone.paymentservice.service.PaymentTransactionService;
import com.capstone.paymentservice.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final PaymentService paymentService;
    private final PaymentTransactionService paymentTransactionService;
    private final PayoutService payoutService;

    @PostMapping(path = "/cancel-payment/{orderCode}")
    public ResponseEntity<BaseResponse<Boolean>> cancelPayment(
            @PathVariable String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Hủy thanh toán thành công", paymentService.cancelPayment(orderCode)));
    }

    @GetMapping("/payment/{orderCode}")
    public ResponseEntity<BaseResponse<PaymentTransactionResponse>> getPaymentInfo(
            @PathVariable String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Lấy trạng thái thanh toán thành công", paymentTransactionService.getTransactionByOrderCode(orderCode)));
    }

    @PostMapping(path = "/payment/create")
    public ResponseEntity<BaseResponse<PaymentLinkResponse>> createPaymentLink(
            @RequestBody OrderInternalResponse request
    ) {
        log.info("Request to create payment link: {}", request);
        return ResponseEntity.ok(BaseResponse.created("Tạo thanh toán thành công", paymentService.getPaymentLink(request)));
    }

    @PostMapping(path = "/payment/resale/pay-out")
    public ResponseEntity<BaseResponse<Boolean>> processResalePayout(
            @RequestBody ResalePayoutRequest request
    ) {
        log.info("Request to process resale payout: {}", request);
        boolean success = payoutService.createPayout(request.getAmount(), request.getBinCode(), request.getBankAccountNumber());
        return ResponseEntity.ok(BaseResponse.ok(success ? "Xử lý thanh toán lại thành công" : "Xử lý thanh toán lại thất bại", success));
    }
}
