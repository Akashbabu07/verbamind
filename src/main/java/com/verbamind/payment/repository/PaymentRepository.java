package com.verbamind.payment.repository;

import com.verbamind.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Page<Payment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}