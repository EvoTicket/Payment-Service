package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderFeignClient;
import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.dto.response.PaymentLinkResponse;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import com.capstone.paymentservice.service.strategy.PaymentResult;
import com.capstone.paymentservice.service.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import vn.payos.PayOS;

import java.util.*;

@Slf4j
@Service
public class PaymentService {
    private final OrderFeignClient orderFeignClient;
    private final Map<PaymentMethod, PaymentStrategy> strategyMap;
    private final PayOS payOS;
    private final WebClient sepayWebClient;
    private final PaymentTransactionService paymentTransactionService;

    @Value("${back-end.domain}")
    private String backendDomain;

    @Value("${sepay.secret-key}")
    private String sePaySecretKey;

    @Value("${sepay.merchant-id}")
    private String sePayMerchantId;

    public PaymentService(
            OrderFeignClient orderFeignClient,
            List<PaymentStrategy> strategies,
            PayOS payOS,
            WebClient sepayWebClient,
            PaymentTransactionService paymentTransactionService
    ) {
        this.orderFeignClient = orderFeignClient;
        this.sepayWebClient = sepayWebClient;
        this.paymentTransactionService = paymentTransactionService;
        this.strategyMap = new EnumMap<>(PaymentMethod.class);
        this.payOS = payOS;

        for (PaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.getPaymentMethod(), strategy);
        }
    }

    public PaymentLinkResponse getPaymentLink(String orderCode) {
        OrderInternalResponse order = orderFeignClient.getOrderDetail(orderCode).getData();
        PaymentMethod method = order.getPaymentMethod();

        PaymentStrategy strategy = strategyMap.get(method);
        if (strategy == null) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không hỗ trợ phương thức thanh toán: " + method);
        }

        PaymentResult result = strategy.createPayment(order);

        String redirectUrl;
        if (result.isFormBased()) {
            // SePay: redirect tới page render form auto-submit
            redirectUrl = backendDomain + "/payment-service/payment/sepay?orderCode=" + orderCode;
        } else {
            redirectUrl = result.getRedirectUrl();
        }

        return PaymentLinkResponse.builder().redirectUrl(redirectUrl).build();
    }

    public PaymentResult createSePayPayment(String orderCode) {
        OrderInternalResponse order = orderFeignClient.getOrderDetail(orderCode).getData();
        PaymentStrategy strategy = strategyMap.get(PaymentMethod.SEPAY);
        if (strategy == null) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "SePay payment strategy not configured");
        }
        return strategy.createPayment(order);
    }

    public boolean cancelPayment(String orderCode) {
        OrderInternalResponse order = orderFeignClient.getOrderDetail(orderCode).getData();
        PaymentMethod method = order.getPaymentMethod();
        boolean canCancel = paymentTransactionService.cancelTransaction(orderCode);
        if(canCancel) {
            switch (method) {
                case PAYOS -> payOS.paymentRequests().cancel(Long.parseLong(orderCode), "");
                case SEPAY -> cancelSePayOrder(orderCode);
            }
        }
        return canCancel;
    }

    public void cancelSePayOrder(String orderInvoiceNumber) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((sePayMerchantId + ":" + sePaySecretKey).getBytes());

        Map<String, String> body = new HashMap<>();
        body.put("order_invoice_number", orderInvoiceNumber);

        sepayWebClient.post()
                .uri("/v1/order/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
