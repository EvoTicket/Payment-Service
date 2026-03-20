package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.event.PaymentSuccessEvent;
import com.capstone.paymentservice.dto.request.SePayIPNRequest;
import com.capstone.paymentservice.dto.response.SePayResponse;
import com.capstone.paymentservice.producer.RedisStreamProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment/sepay")
@RequiredArgsConstructor
public class SePayRestController {
    private final RedisStreamProducer redisStreamProducer;

    @Value("${sepay.secret-key}")
    private String secretKey;

    @PostMapping("/init")
    public ResponseEntity<BaseResponse<SePayResponse>> initPayment(
            @RequestParam Long orderId
    ) {
        String redirectUrl = "/payment-service/payment/sepay?orderId=" + orderId;

        return ResponseEntity.ok(BaseResponse.ok("Tạo thanh toán thành công", new SePayResponse(redirectUrl))
        );
    }


    @PostMapping("/ipn")
    public ResponseEntity<?> handleIpn(
            @RequestHeader(value = "X-Secret-Key", required = false) String headerKey,
            @RequestBody SePayIPNRequest request
    ) {
        if (headerKey == null || !headerKey.equals(secretKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        log.info("IPN received request: {}", request);

        if ("ORDER_PAID".equals(request.getNotificationType())) {
            String orderCode = request.getOrder().getOrderInvoiceNumber();
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .orderCode(Long.valueOf(orderCode))
                    .description(request.getOrder().getOrderDescription())
                    .amount(Long.valueOf(request.getOrder().getOrderAmount()))
                    .transactionDateTime(request.getTransaction().getTransactionDate())
                    .transactionId(request.getTransaction().getTransactionId())
                    .build();
            redisStreamProducer.sendMessage("payment-success", event);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}
