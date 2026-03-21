package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

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

        Map<String, Object> fields = paymentService.createPaymentFields(orderCode);
        model.addAllAttributes(fields);

        return "sepay-form";
    }
}
