package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/sepay")
@RequiredArgsConstructor
public class SePayRestController {

    @PostMapping("/init")
    public ResponseEntity<BaseResponse<Map<String, String>>> initPayment(
            @RequestParam Long orderId
    ) {
        String redirectUrl = "/payment-service/payment/sepay?orderId=" + orderId;

        return ResponseEntity.ok(BaseResponse.ok(
                        "Tạo thanh toán thành công",
                        Map.of("redirectUrl", redirectUrl)
                )
        );
    }
}
