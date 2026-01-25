package com.capstone.paymentservice.client;

import com.capstone.paymentservice.config.FeignClientConfig;
import com.capstone.paymentservice.dto.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "order-service",
        path = "/api/internal",
        configuration = FeignClientConfig.class
)
public interface OrderFeignClient {
    @GetMapping("/orders/detail")
    BaseResponse<OrderInternalResponse> getOrderDetail(@RequestParam("orderId") Long orderId);
}
