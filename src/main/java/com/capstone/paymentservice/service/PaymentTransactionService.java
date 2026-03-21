package com.capstone.paymentservice.service;

import com.capstone.paymentservice.dto.event.PaymentSuccessEvent;
import com.capstone.paymentservice.entity.PaymentTransaction;
import com.capstone.paymentservice.enums.EventPublishStatus;
import com.capstone.paymentservice.enums.PaymentMethod;
import com.capstone.paymentservice.enums.PaymentStatus;
import com.capstone.paymentservice.exception.AppException;
import com.capstone.paymentservice.exception.ErrorCode;
import com.capstone.paymentservice.producer.RedisStreamProducer;
import com.capstone.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RedisStreamProducer redisStreamProducer;

    private static final String PAYMENT_SUCCESS_STREAM = "payment-success";

    @Transactional
    public void createPendingTransaction(
            String orderCode,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String buyerName,
            String buyerEmail,
            String buyerPhone,
            String description,
            String paymentLinkUrl
    ) {
        Optional<PaymentTransaction> existing = paymentTransactionRepository.findByOrderCode(orderCode);
        if (existing.isPresent()) {
            PaymentTransaction existingTx = existing.get();
            if (existingTx.getStatus() == PaymentStatus.SUCCESS) {
                throw new AppException(ErrorCode.CONFLICT, "Đơn hàng này đã được thanh toán thành công");
            }
            // Nếu đã có PENDING/FAILED, cập nhật lại thông tin mới
            existingTx.setPaymentMethod(paymentMethod);
            existingTx.setAmount(amount);
            existingTx.setPaymentLinkUrl(paymentLinkUrl);
            existingTx.setDescription(description);
            existingTx.setStatus(PaymentStatus.PENDING);
            existingTx.setFailureReason(null);
            log.info("Updated existing PENDING transaction for orderCode={}", orderCode);
            paymentTransactionRepository.save(existingTx);
            return;
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderCode(orderCode)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .buyerName(buyerName)
                .buyerEmail(buyerEmail)
                .buyerPhone(buyerPhone)
                .description(description)
                .paymentLinkUrl(paymentLinkUrl)
                .eventPublishStatus(EventPublishStatus.NOT_PUBLISHED)
                .retryCount(0)
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        log.info("Created PENDING payment transaction id={} for orderCode={}, method={}",
                saved.getId(), orderCode, paymentMethod);
    }

    /**
     * Xử lý payment success từ webhook PayOS.
     * Thực hiện idempotency check: nếu transaction đã được xử lý (SUCCESS), skip.
     * Cập nhật trạng thái và publish event tới Redis.
     *
     * @return true nếu đây là lần xử lý đầu tiên, false nếu đã xử lý trước đó (duplicate)
     */
    @Transactional
    public boolean handlePaymentSuccess(
            String orderCode,
            String transactionId,
            String transactionDateTime
    ) {
        // Idempotency check: đã có transaction SUCCESS với transactionId này chưa?
        if (transactionId != null && paymentTransactionRepository.existsByTransactionIdAndStatus(transactionId, PaymentStatus.SUCCESS)) {
            log.warn("Duplicate webhook detected: transactionId={} already processed as SUCCESS. Skipping.", transactionId);
            return false;
        }

        // Tìm transaction PENDING theo orderCode
        Optional<PaymentTransaction> optionalTx = paymentTransactionRepository.findByOrderCode(orderCode);

        PaymentTransaction transaction;
        if (optionalTx.isPresent()) {
            transaction = optionalTx.get();
            if (transaction.getStatus() == PaymentStatus.SUCCESS) {
                log.warn("Duplicate webhook detected: orderCode={} already SUCCESS. Skipping.", orderCode);
                return false;
            }
        } else {
            // Webhook đến trước khi có record PENDING (edge case)
            log.warn("No PENDING transaction found for orderCode={}. Creating record from webhook data.", orderCode);
            transaction = PaymentTransaction.builder()
                    .orderCode(orderCode)
                    .amount(BigDecimal.ZERO)
                    .paymentMethod(PaymentMethod.PAYOS)
                    .status(PaymentStatus.PENDING)
                    .eventPublishStatus(EventPublishStatus.NOT_PUBLISHED)
                    .retryCount(0)
                    .build();
        }

        // Cập nhật trạng thái SUCCESS
        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setTransactionId(transactionId);
        transaction.setTransactionDateTime(transactionDateTime);

        // Publish event tới Redis
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderCode(orderCode)
                .transactionDateTime(transactionDateTime)
                .transactionId(transactionId)
                .build();

        try {
            redisStreamProducer.sendMessage(PAYMENT_SUCCESS_STREAM, event);
            transaction.setEventPublishStatus(EventPublishStatus.PUBLISHED);
            log.info("Payment success event published for orderCode={}, transactionId={}", orderCode, transactionId);
        } catch (Exception e) {
            transaction.setEventPublishStatus(EventPublishStatus.FAILED);
            transaction.setFailureReason("Redis publish failed: " + e.getMessage());
            log.error("Failed to publish payment success event for orderCode={}. Will retry later.", orderCode, e);
        }

        paymentTransactionRepository.save(transaction);
        return true;
    }

    /**
     * Xử lý SePay payment success (tương tự PayOS nhưng set PaymentMethod = SEPAY).
     */
    @Transactional
    public boolean handleSePayPaymentSuccess(
            String orderCode,
            String transactionId,
            String transactionDateTime
    ) {
        // Idempotency check
        if (transactionId != null && paymentTransactionRepository.existsByTransactionIdAndStatus(transactionId, PaymentStatus.SUCCESS)) {
            log.warn("Duplicate SePay IPN detected: transactionId={} already processed as SUCCESS. Skipping.", transactionId);
            return false;
        }

        Optional<PaymentTransaction> optionalTx = paymentTransactionRepository.findByOrderCode(orderCode);

        PaymentTransaction transaction;
        if (optionalTx.isPresent()) {
            transaction = optionalTx.get();
            if (transaction.getStatus() == PaymentStatus.SUCCESS) {
                log.warn("Duplicate SePay IPN detected: orderCode={} already SUCCESS. Skipping.", orderCode);
                return false;
            }
        } else {
            log.warn("No PENDING transaction found for SePay orderCode={}. Creating record from IPN data.", orderCode);
            transaction = PaymentTransaction.builder()
                    .orderCode(orderCode)
                    .amount(BigDecimal.ZERO)
                    .paymentMethod(PaymentMethod.SEPAY)
                    .status(PaymentStatus.PENDING)
                    .eventPublishStatus(EventPublishStatus.NOT_PUBLISHED)
                    .retryCount(0)
                    .build();
        }

        transaction.setStatus(PaymentStatus.SUCCESS);
        transaction.setTransactionId(transactionId);
        transaction.setTransactionDateTime(transactionDateTime);

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderCode(orderCode)
                .transactionDateTime(transactionDateTime)
                .transactionId(transactionId)
                .build();

        try {
            redisStreamProducer.sendMessage(PAYMENT_SUCCESS_STREAM, event);
            transaction.setEventPublishStatus(EventPublishStatus.PUBLISHED);
            log.info("SePay payment success event published for orderCode={}, transactionId={}", orderCode, transactionId);
        } catch (Exception e) {
            transaction.setEventPublishStatus(EventPublishStatus.FAILED);
            transaction.setFailureReason("Redis publish failed: " + e.getMessage());
            log.error("Failed to publish SePay payment success event for orderCode={}. Will retry later.", orderCode, e);
        }

        paymentTransactionRepository.save(transaction);
        return true;
    }

    /**
     * Cập nhật trạng thái CANCELLED.
     */
    @Transactional
    public void cancelTransaction(String orderCode) {
        paymentTransactionRepository.findByOrderCode(orderCode)
                .ifPresent(tx -> {
                    if (tx.getStatus() == PaymentStatus.PENDING) {
                        tx.setStatus(PaymentStatus.CANCELLED);
                        paymentTransactionRepository.save(tx);
                        log.info("Payment transaction cancelled for orderCode={}", orderCode);
                    }
                });
    }

    /**
     * Lấy trạng thái payment theo orderCode.
     */
    @Transactional(readOnly = true)
    public PaymentTransaction getByOrderCode(String orderCode) {
        return paymentTransactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy giao dịch thanh toán cho mã đơn hàng " + orderCode));
    }

    /**
     * Retry publish event cho các transaction bị fail.
     * Được gọi bởi scheduled job.
     */
    @Transactional
    public void retryFailedEvents(int maxRetryCount) {
        List<PaymentTransaction> failedTransactions = paymentTransactionRepository
                .findByEventPublishStatusAndRetryCountLessThan(EventPublishStatus.FAILED, maxRetryCount);

        for (PaymentTransaction tx : failedTransactions) {
            try {
                PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                        .orderCode(tx.getOrderCode())
                        .transactionDateTime(tx.getTransactionDateTime())
                        .transactionId(tx.getTransactionId())
                        .build();

                redisStreamProducer.sendMessage(PAYMENT_SUCCESS_STREAM, event);

                tx.setEventPublishStatus(EventPublishStatus.PUBLISHED);
                tx.setFailureReason(null);
                paymentTransactionRepository.save(tx);

                log.info("Retry successful: event published for orderCode={}, attempt={}",
                        tx.getOrderCode(), tx.getRetryCount() + 1);
            } catch (Exception e) {
                tx.setRetryCount(tx.getRetryCount() + 1);
                tx.setFailureReason("Retry #" + tx.getRetryCount() + " failed: " + e.getMessage());
                paymentTransactionRepository.save(tx);

                log.error("Retry failed for orderCode={}, attempt={}/{}",
                        tx.getOrderCode(), tx.getRetryCount(), maxRetryCount, e);
            }
        }
    }
}
