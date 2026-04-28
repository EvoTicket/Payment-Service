package com.capstone.paymentservice.client;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.PaymentLinkResponse;
import com.capstone.paymentservice.dto.response.PaymentTransactionResponse;
import com.capstone.paymentservice.service.PaymentService;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final PaymentService paymentService;
    private final PaymentTransactionService paymentTransactionService;

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

    @PostMapping(path = "/payment/create/{orderCode}")
    public ResponseEntity<BaseResponse<PaymentLinkResponse>> createPaymentLink(
            @PathVariable String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.created("Tạo thanh toán thành công", paymentService.getPaymentLink(orderCode)));
    }
}
