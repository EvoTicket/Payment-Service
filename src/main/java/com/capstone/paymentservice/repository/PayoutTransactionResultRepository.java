package com.capstone.paymentservice.repository;

import com.capstone.paymentservice.entity.PayoutTransactionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutTransactionResultRepository extends JpaRepository<PayoutTransactionResult, String> {
}
