package com.capstone.paymentservice.config;

import com.capstone.paymentservice.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrySchedulerConfig {

    private final PaymentTransactionService paymentTransactionService;

    @Value("${payment.retry.max-count:5}")
    private int maxRetryCount;

    /**
     * Chạy mỗi 60 giây để retry publish event cho các transaction bị fail.
     */
    @Scheduled(fixedDelayString = "${payment.retry.interval-ms:60000}")
    public void retryFailedEventPublishing() {
        log.debug("Running scheduled retry for failed event publishing...");
        try {
            paymentTransactionService.retryFailedEvents(maxRetryCount);
        } catch (Exception e) {
            log.error("Error during scheduled retry of failed events", e);
        }
    }
}
