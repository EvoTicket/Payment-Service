package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.PaymentLinkResponse;
import com.capstone.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping(path = "/create")
    public ResponseEntity<BaseResponse<PaymentLinkResponse>> createPaymentLink(
            @RequestParam String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.created("Tạo thanh toán thành công", paymentService.getPaymentLink(orderCode)));
    }
}
