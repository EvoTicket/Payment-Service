package com.capstone.paymentservice.service.strategy;

import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayOSPaymentStrategy implements PaymentStrategy {

    private final PayOS payOS;
    private final PaymentTransactionService paymentTransactionService;

    @Value("${front-end.domain}")
    private String frontendDomain;

    @Value("${back-end.domain}")
    private String backendDomain;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYOS;
    }

    @Override
    public PaymentResult createPayment(OrderInternalResponse order) {
        String description = "Ma don hang " + order.getOrderCode();

        List<PaymentLinkItem> items = order.getItems()
                .stream()
                .map(item -> PaymentLinkItem.builder()
                        .name(item.getTicketTypeName())
                        .quantity(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                        .price(item.getSubTotal().longValue())
                        .build()
                )
                .toList();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(Long.valueOf(order.getOrderCode()))
                .description(description)
                .amount(order.getFinalAmount().longValue())
                .items(items)
                .buyerName(order.getBuyerName())
                .buyerEmail(order.getBuyerEmail())
                .buyerPhone(order.getBuyerPhone())
                .returnUrl(frontendDomain + "/" + order.getLocale() + "/user/events/" + order.getEventId() + "/payment/result?orderCode=" + order.getOrderCode())
                .cancelUrl(backendDomain + "/order-service/api/v1/orders/cancel-callback?orderCode=" + order.getOrderCode() + "&eventId=" + order.getEventId() + "&locale=" + order.getLocale())
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);

        paymentTransactionService.createPendingTransaction(
                order.getOrderCode(),
                order.getFinalAmount(),
                PaymentMethod.PAYOS,
                order.getBuyerName(),
                order.getBuyerEmail(),
                order.getBuyerPhone(),
                description,
                response.getCheckoutUrl()
        );

        return PaymentResult.redirect(response.getCheckoutUrl());
    }
}
