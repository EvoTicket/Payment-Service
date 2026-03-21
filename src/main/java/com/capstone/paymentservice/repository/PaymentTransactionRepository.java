package com.capstone.paymentservice.repository;

import com.capstone.paymentservice.entity.PaymentTransaction;
import com.capstone.paymentservice.enums.EventPublishStatus;
import com.capstone.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    boolean existsByTransactionIdAndStatus(String transactionId, PaymentStatus status);

    Optional<PaymentTransaction> findByOrderCode(String orderCode);

    List<PaymentTransaction> findByOrderCodeAndStatus(String orderCode, PaymentStatus status);

    List<PaymentTransaction> findByEventPublishStatusAndRetryCountLessThan(
            EventPublishStatus eventPublishStatus, int maxRetryCount
    );
}
