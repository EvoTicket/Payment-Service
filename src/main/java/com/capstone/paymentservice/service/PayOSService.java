package com.capstone.paymentservice.service;

import com.capstone.paymentservice.client.OrderFeignClient;
import com.capstone.paymentservice.client.OrderInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayOSService {
    private final PayOS payOS;
    private final OrderFeignClient orderFeignClient;

    @Value("${front-end.cancel-url}")
    private String cancelUrl;

    @Value("${front-end.return-url}")
    private String returnUrl;


    public CreatePaymentLinkResponse createPaymentLink(
            Long orderId
    ) {
        OrderInternalResponse orderInternalResponse = orderFeignClient.getOrderDetail(orderId).getData();

        String description = "Thanh toan cho đdn hang " + orderInternalResponse.getOrderCode()
                + " voi su kien " + orderInternalResponse.getEventName();

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
        return payOS.paymentRequests().create(paymentData);
    }
}
