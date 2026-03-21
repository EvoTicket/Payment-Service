package com.capstone.paymentservice.controller.payos;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {
  private final PayOS payOS;
  private final PaymentTransactionService paymentTransactionService;

  @PostMapping(path = "/confirm")
  public BaseResponse<WebhookData> payosTransferHandler(@RequestBody Webhook webhook)
          throws IllegalArgumentException {
    WebhookData data = payOS.webhooks().verify(webhook);
    log.info("PayOS webhook received: orderCode={}, success={}", data.getOrderCode(), webhook.getSuccess());

    if (webhook.getSuccess()) {
      boolean isNewEvent = paymentTransactionService.handlePaymentSuccess(
              data.getOrderCode(),
              data.getReference(),
              data.getTransactionDateTime()
      );

      if (!isNewEvent) {
        log.info("Duplicate PayOS webhook for orderCode={}, already processed", data.getOrderCode());
      }
    }

    return BaseResponse.ok("Webhook delivered", data);
  }
}
