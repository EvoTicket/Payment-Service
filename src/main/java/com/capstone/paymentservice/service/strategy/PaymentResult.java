package com.capstone.paymentservice.service.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Kết quả tạo payment từ strategy.
 * - redirectUrl: URL redirect trực tiếp (PayOS)
 * - formFields: form data cần auto-submit (SePay)
 *
 * Chỉ 1 trong 2 sẽ có giá trị, tùy payment method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    /** URL redirect trực tiếp tới payment gateway (dùng cho PayOS) */
    private String redirectUrl;

    /** Form fields cho auto-submit form (dùng cho SePay) */
    private Map<String, Object> formFields;

    public boolean isFormBased() {
        return formFields != null && !formFields.isEmpty();
    }

    public static PaymentResult redirect(String url) {
        return PaymentResult.builder().redirectUrl(url).build();
    }

    public static PaymentResult form(Map<String, Object> fields) {
        return PaymentResult.builder().formFields(fields).build();
    }
}
