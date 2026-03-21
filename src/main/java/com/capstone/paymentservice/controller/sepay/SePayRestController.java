package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.request.SePayIPNRequest;
import com.capstone.paymentservice.dto.response.SePayResponse;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment/sepay")
@RequiredArgsConstructor
public class SePayRestController {
    private final PaymentTransactionService paymentTransactionService;

    @Value("${sepay.secret-key}")
    private String secretKey;

    @Value("${back-end.domain}")
    private String backendDomain;

    @PostMapping("/init")
    public ResponseEntity<BaseResponse<SePayResponse>> initPayment(
            @RequestParam Long orderCode
    ) {
        String redirectUrl = backendDomain + "/payment-service/payment/sepay?orderCode=" + orderCode;

        return ResponseEntity.ok(BaseResponse.ok("Tạo thanh toán thành công", new SePayResponse(redirectUrl))
        );
    }


    @PostMapping("/ipn")
    public ResponseEntity<?> handleIpn(
            @RequestHeader(value = "X-Secret-Key", required = false) String headerKey,
            @RequestBody SePayIPNRequest request
    ) {
        // Constant-time comparison để tránh timing attack
        if (headerKey == null || !MessageDigest.isEqual(
                headerKey.getBytes(), secretKey.getBytes())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        log.info("SePay IPN received: notificationType={}, orderInvoiceNumber={}",
                request.getNotificationType(),
                request.getOrder() != null ? request.getOrder().getOrderInvoiceNumber() : "null");

        if ("ORDER_PAID".equals(request.getNotificationType())) {
            String orderCode = request.getOrder().getOrderInvoiceNumber();

            boolean isNewEvent = paymentTransactionService.handleSePayPaymentSuccess(
                    Long.valueOf(orderCode),
                    request.getTransaction().getTransactionId(),
                    request.getTransaction().getTransactionDate()
            );

            if (!isNewEvent) {
                log.info("Duplicate SePay IPN for orderCode={}, already processed", orderCode);
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}
