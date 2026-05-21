package com.capstone.paymentservice.repository;

import com.capstone.paymentservice.entity.PayoutResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayoutResultRepository extends JpaRepository<PayoutResult, String> {
}
