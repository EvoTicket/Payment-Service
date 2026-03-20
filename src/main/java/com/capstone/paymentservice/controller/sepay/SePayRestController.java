package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.response.SePayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payment/sepay")
@RequiredArgsConstructor
public class SePayRestController {

    @PostMapping("/init")
    public ResponseEntity<BaseResponse<SePayResponse>> initPayment(
            @RequestParam Long orderId
    ) {
        String redirectUrl = "/payment-service/payment/sepay?orderId=" + orderId;

        return ResponseEntity.ok(BaseResponse.ok("Tạo thanh toán thành công", new SePayResponse(redirectUrl))
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<BaseResponse<String>> webhook(
            @RequestBody Object payload
    ) {
        log.info("Payload: {}", payload);
        return ResponseEntity.ok(BaseResponse.ok("sepay webhook", null));
    }
}
