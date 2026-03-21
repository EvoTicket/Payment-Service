package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderFeignClient;
import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {
    private final PayOS payOS;
    private final OrderFeignClient orderFeignClient;
    private final PaymentTransactionService paymentTransactionService;

    @Value("${front-end.cancel-url}")
    private String cancelUrl;

    @Value("${front-end.return-url}")
    private String returnUrl;


    public CreatePaymentLinkResponse createPaymentLink(String orderCode) {
        OrderInternalResponse orderInternalResponse = orderFeignClient.getOrderDetail(orderCode).getData();

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
                Long.valueOf(orderInternalResponse.getOrderCode()),
                orderInternalResponse.getFinalAmount(),
                PaymentMethod.PAYOS,
                orderInternalResponse.getBuyerName(),
                orderInternalResponse.getBuyerEmail(),
                orderInternalResponse.getBuyerPhone(),
                description,
                response.getCheckoutUrl()
        );

        return response;
    }
}
