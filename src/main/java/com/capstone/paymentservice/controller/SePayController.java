package com.capstone.paymentservice.controller;

import com.capstone.paymentservice.service.PaymentService;
import com.capstone.paymentservice.service.strategy.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/payment/sepay")
@RequiredArgsConstructor
public class SePayController {
    private final PaymentService paymentService;

    @GetMapping
    public String payment(
            @RequestParam String orderCode,
            Model model
    ) {
        PaymentResult result = paymentService.createSePayPayment(orderCode);
        model.addAllAttributes(result.getFormFields());

        return "sepay-form";
    }
}
