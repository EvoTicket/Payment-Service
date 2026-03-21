package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderFeignClient;
import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.dto.response.PaymentLink;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PayOS payOS;
    private final OrderFeignClient orderFeignClient;
    private final PaymentTransactionService paymentTransactionService;

    @Value("${front-end.cancel-url}")
    private String cancelUrl;

    @Value("${front-end.return-url}")
    private String returnUrl;

    @Value("${sepay.secret-key}")
    private String SePaySecretKey;

    @Value("${sepay.merchant-id}")
    private String SePayMerchantId;

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

    public PaymentLink getPaymentLink(String orderCode){
        OrderInternalResponse orderInternalResponse = orderFeignClient.getOrderDetail(orderCode).getData();

        String redirectUrl = switch (orderInternalResponse.getPaymentMethod()){
            case PAYOS ->  createPaymentLinkPayOS(orderInternalResponse);
            case SEPAY -> backendDomain + "/payment-service/payment/sepay?orderCode=" + orderCode;
        };

        return PaymentLink.builder().redirectUrl(redirectUrl).build();
    }

    public String createPaymentLinkPayOS(OrderInternalResponse orderInternalResponse) {
        String description = "Ma don hang " + orderInternalResponse.getOrderCode();

        List<PaymentLinkItem> items = orderInternalResponse.getItems()
                .stream()
                .map(item -> PaymentLinkItem.builder()
                        .name(item.getTicketTypeName())
                        .quantity(item.getQuantity())
                        .price(item.getUnitPrice().longValue())
                        .build()
                )
                .toList();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(Long.valueOf(orderInternalResponse.getOrderCode()))
                .description(description)
                .amount(orderInternalResponse.getFinalAmount().longValue())
                .items(items)
                .buyerName(orderInternalResponse.getBuyerName())
                .buyerEmail(orderInternalResponse.getBuyerEmail())
                .buyerPhone(orderInternalResponse.getBuyerPhone())
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);

        // Lưu payment transaction với trạng thái PENDING
        paymentTransactionService.createPendingTransaction(
                orderInternalResponse.getOrderCode(),
                orderInternalResponse.getFinalAmount(),
                PaymentMethod.PAYOS,
                orderInternalResponse.getBuyerName(),
                orderInternalResponse.getBuyerEmail(),
                orderInternalResponse.getBuyerPhone(),
                description,
                response.getCheckoutUrl()
        );

        return response.getCheckoutUrl();
    }

    public Map<String, Object> createPaymentFields(String orderCode) {
        OrderInternalResponse orderInternalResponse = orderFeignClient.getOrderDetail(orderCode).getData();

        String description = "Ma don hang " + orderInternalResponse.getOrderCode();

        Map<String, Object> fields = new HashMap<>();
        fields.put("merchant", SePayMerchantId);
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

        // Lưu payment transaction với trạng thái PENDING
        String redirectUrl = backendDomain + "/payment-service/payment/sepay?orderCode=" + orderCode;
        paymentTransactionService.createPendingTransaction(
                orderInternalResponse.getOrderCode(),
                orderInternalResponse.getFinalAmount(),
                PaymentMethod.SEPAY,
                orderInternalResponse.getBuyerName(),
                orderInternalResponse.getBuyerEmail(),
                orderInternalResponse.getBuyerPhone(),
                description,
                redirectUrl
        );

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
                    SePaySecretKey.getBytes(StandardCharsets.UTF_8),
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
