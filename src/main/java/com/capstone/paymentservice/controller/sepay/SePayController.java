package com.capstone.paymentservice.controller.sepay;

import com.capstone.paymentservice.service.PaymentTokenService;
import com.capstone.paymentservice.service.SePayService;
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
    private final SePayService sePayService;
    private final PaymentTokenService paymentTokenService;

    /**
     * Nhận token từ query param, validate và consume (one-time use).
     * Nếu token hợp lệ -> lấy orderCode từ Redis -> tạo form SePay -> auto-submit redirect.
     * Nếu token không hợp lệ hoặc hết hạn -> trả lỗi.
     */
    @GetMapping
    public String payment(
            @RequestParam String token,
            Model model
    ) {
        String orderCode = paymentTokenService.validateAndConsumeToken(token);

        if (orderCode == null) {
            log.warn("Invalid or expired payment token: {}", token);
            model.addAttribute("errorMessage", "Link thanh toán không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
            return "sepay-error";
        }

        Map<String, Object> fields = sePayService.createPaymentFields(orderCode);
        model.addAllAttributes(fields);

        return "sepay-form";
    }
}
