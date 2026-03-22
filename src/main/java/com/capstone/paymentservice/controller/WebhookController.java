package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.request.SePayIPNRequest;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.*;

import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private final PayOS payOS;
    private final PaymentTransactionService paymentTransactionService;

    @Value("${sepay.secret-key}")
    private String secretKey;

    @PostMapping(path = "/payos")
    public BaseResponse<WebhookData> payosTransferHandler(@RequestBody Webhook webhook)
            throws IllegalArgumentException {
        WebhookData data = payOS.webhooks().verify(webhook);
        log.info("PayOS webhook received: orderCode={}, success={}", data.getOrderCode(), webhook.getSuccess());

        if (webhook.getSuccess()) {
            boolean isNewEvent = paymentTransactionService.handlePaymentSuccess(
                    String.valueOf(data.getOrderCode()),
                    data.getReference(),
                    data.getTransactionDateTime(),
                    PaymentMethod.PAYOS
            );

            if (!isNewEvent) {
                log.info("Duplicate PayOS webhook for orderCode={}, already processed", data.getOrderCode());
            }
        }

        return BaseResponse.ok("Webhook delivered", data);
    }

    @PostMapping("/sepay")
    public ResponseEntity<?> handleIpn(
            @RequestHeader(value = "X-Secret-Key", required = false) String headerKey,
            @RequestBody SePayIPNRequest request
    ) {
        if (headerKey == null || !MessageDigest.isEqual(headerKey.getBytes(), secretKey.getBytes())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        log.info("SePay IPN received: notificationType={}, orderInvoiceNumber={}",
                request.getNotificationType(),
                request.getOrder() != null ? request.getOrder().getOrderInvoiceNumber() : "null");

        if ("ORDER_PAID".equals(request.getNotificationType())) {
            boolean isNewEvent = paymentTransactionService.handlePaymentSuccess(
                    request.getOrder().getOrderInvoiceNumber(),
                    request.getTransaction().getTransactionId(),
                    request.getTransaction().getTransactionDate(),
                    PaymentMethod.SEPAY
            );

            if (!isNewEvent) {
                log.info("Duplicate SePay IPN for orderCode={}, already processed", request.getOrder().getOrderInvoiceNumber());
            }
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}
