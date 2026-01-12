package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.type.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.*;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {
  private final PayOS payOS;

  public PaymentController(PayOS payOS) {
    super();
    this.payOS = payOS;
  }

  @PostMapping(path = "/payos_transfer_handler")
  public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Object body)
      throws JsonProcessingException, IllegalArgumentException {
    try {
      WebhookData data = payOS.webhooks().verify(body);
      System.out.println(data);
      log.info("webhook");
      return ApiResponse.success("Webhook delivered", data);
    } catch (Exception e) {
      e.printStackTrace();
      return ApiResponse.error(e.getMessage());
    }
  }
}
