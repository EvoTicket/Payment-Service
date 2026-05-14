package com.capstone.paymentservice.service.strategy;

import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SePayPaymentStrategy implements PaymentStrategy {

    private final PaymentTransactionService paymentTransactionService;

    @Value("${sepay.secret-key}")
    private String sePaySecretKey;

    @Value("${sepay.merchant-id}")
    private String sePayMerchantId;

    @Value("${front-end.domain}")
    private String frontendDomain;

    @Value("${back-end.domain}")
    private String backendDomain;

    private static final List<String> SIGNED_FIELDS = List.of(
            "merchant",
            "operation",
            "payment_method",
            "order_amount",
            "currency",
            "order_invoice_number",
            "order_description",
            "customer_id",
            "success_url",
            "error_url",
            "cancel_url"
    );

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.SEPAY;
    }

    @Override
    public PaymentResult createPayment(OrderInternalResponse order) {
        String description = "Ma don hang " + order.getOrderCode();

        Map<String, Object> fields = new HashMap<>();
        fields.put("merchant", sePayMerchantId);
        fields.put("currency", "VND");
        fields.put("order_amount", order.getFinalAmount());
        fields.put("operation", "PURCHASE");
        fields.put("order_description", description);
        fields.put("order_invoice_number", order.getOrderCode());
        fields.put("customer_id", order.getBuyerName());
        fields.put("success_url", frontendDomain + "/" + order.getLocale() + "/user/events/" + order.getEventId() + "/payment/result?status=PAID&orderCode=" + order.getOrderCode());
        fields.put("error_url", frontendDomain + "/" + order.getLocale() + "/user/events/" + order.getEventId() + "/payment/result?status=failed");
        fields.put("cancel_url", frontendDomain + "/" + order.getLocale() + "/user/events/" + order.getEventId() + "/payment/result?status=cancelled");

        fields.put("signature", signFields(fields));

        // Lưu payment transaction với trạng thái PENDING
        String redirectUrl = backendDomain + "/payment-service/payment/sepay?orderCode=" + order.getOrderCode();
        paymentTransactionService.createPendingTransaction(
                order.getOrderCode(),
                order.getFinalAmount(),
                PaymentMethod.SEPAY,
                order.getBuyerName(),
                order.getBuyerEmail(),
                order.getBuyerPhone(),
                description,
                redirectUrl
        );

        return PaymentResult.form(fields);
    }

    private String signFields(Map<String, Object> fields) {
        try {
            List<String> signed = SIGNED_FIELDS.stream()
                    .filter(fields::containsKey)
                    .map(field -> field + "=" + Objects.toString(fields.get(field), ""))
                    .toList();

            String data = String.join(",", signed);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    sePaySecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(rawHmac);

        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error while signing fields");
        }
    }
}
