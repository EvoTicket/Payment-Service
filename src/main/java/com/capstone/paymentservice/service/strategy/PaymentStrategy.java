package com.capstone.paymentservice.service.strategy;

import com.capstone.paymentservice.client.OrderInternalResponse;
import com.capstone.paymentservice.enums.PaymentMethod;

/**
 * Strategy interface cho từng phương thức thanh toán.
 * Mỗi implementation chịu trách nhiệm tạo payment link/form data
 * và lưu pending transaction tương ứng.
 */
public interface PaymentStrategy {

    /**
     * Phương thức thanh toán mà strategy này xử lý.
     */
    PaymentMethod getPaymentMethod();

    /**
     * Tạo payment result cho order.
     *
     * @param order thông tin order từ Order Service
     * @return kết quả thanh toán (redirect URL hoặc form data tùy payment method)
     */
    PaymentResult createPayment(OrderInternalResponse order);
}
