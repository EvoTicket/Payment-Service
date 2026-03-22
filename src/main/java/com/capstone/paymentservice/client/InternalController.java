package com.capstone.paymentservice.client;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final PaymentService paymentService;

    @PostMapping(path = "/cancel-payment/{orderCode}")
    public ResponseEntity<BaseResponse<Boolean>> cancelPayment(
            @PathVariable String orderCode
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Hủy thanh toán thành công", paymentService.cancelPayment(orderCode)));
    }
}
