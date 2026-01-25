package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderFeignClient;
import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SePayService {

    @Value("${sepay.secret-key}")
    private String secretKey;

    @Value("${sepay.merchant-id}")
    private String merchantId;

    @Value("${front-end.cancel-url}")
    private String cancelUrl;

    @Value("${front-end.return-url}")
    private String returnUrl;

    private final OrderFeignClient orderFeignClient;

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

    public Map<String, Object> createPaymentFields(Long orderId) {
        OrderInternalResponse orderInternalResponse =
                orderFeignClient.getOrderDetail(orderId).getData();

        String description = "Thanh toan cho don hang "
                + orderInternalResponse.getOrderCode()
                + " voi su kien " + orderInternalResponse.getEventName();

        Map<String, Object> fields = new HashMap<>();
        fields.put("merchant", merchantId);
        fields.put("currency", "VND");
        fields.put("order_amount", orderInternalResponse.getFinalAmount());
        fields.put("operation", "PURCHASE");
        fields.put("order_description", description);
        fields.put("order_invoice_number", orderInternalResponse.getOrderCode());
        fields.put("customer_id", orderInternalResponse.getBuyerName());
        fields.put("success_url", returnUrl);
        fields.put("error_url", returnUrl);
        fields.put("cancel_url", cancelUrl);

        fields.put("signature", signFields(fields));

        return fields;
    }

    public String signFields(Map<String, Object> fields) {
        try {
            List<String> signed = SIGNED_FIELDS.stream()
                    .filter(fields::containsKey)
                    .map(field -> field + "=" + Objects.toString(fields.get(field), ""))
                    .toList();

            String data = String.join(",", signed);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
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
