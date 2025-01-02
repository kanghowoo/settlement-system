package com.settlementsystem.payment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.settlementsystem.payment.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByImpUid(String impUid);
    List<Payment> findByPaymentDateBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, String status);
}
