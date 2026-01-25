package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.service.SePayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/payment/sepay")
@RequiredArgsConstructor
public class SePayController {
    private final SePayService sePayService;

    @GetMapping
    public String payment(
            @RequestParam Long orderId,
            Model model
    ) {
        Map<String, Object> fields = sePayService.createPaymentFields(orderId);

        model.addAllAttributes(fields);

        return "sepay-form";
    }
}
