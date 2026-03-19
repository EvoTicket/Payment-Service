package com.capstone.paymentservice.controller.payos;

import com.capstone.paymentservice.dto.BaseResponse;
import com.capstone.paymentservice.dto.event.PaymentSuccessEvent;
import com.capstone.paymentservice.producer.RedisStreamProducer;
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
  private final RedisStreamProducer redisStreamProducer;

  @PostMapping(path = "/confirm")
  public BaseResponse<WebhookData> payosTransferHandler(@RequestBody Webhook webhook)
          throws IllegalArgumentException {
    WebhookData data = payOS.webhooks().verify(webhook);
    log.info("Webhook: {}", webhook);
    if(webhook.getSuccess()){
      PaymentSuccessEvent event = PaymentSuccessEvent.builder()
              .orderCode(data.getOrderCode())
              .description(data.getDescription())
              .amount(data.getAmount())
              .transactionDateTime(data.getTransactionDateTime())
              .transactionId(data.getReference())
              .build();
      redisStreamProducer.sendMessage("payment-success", event);
    }
    return BaseResponse.ok("Webhook delivered", data);
  }
}
