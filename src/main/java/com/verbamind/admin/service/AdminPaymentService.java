package com.verbamind.admin.service;

import com.verbamind.admin.dto.AdminPaymentPageResponse;
import com.verbamind.admin.dto.AdminPaymentResponse;
import com.verbamind.payment.entity.Payment;
import com.verbamind.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    public AdminPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public AdminPaymentPageResponse listAllPayments(Pageable pageable) {
        Page<Payment> page = paymentRepository.findAllByOrderByCreatedAtDesc(pageable);
        var items = page.getContent().stream().map(this::toResponse).toList();
        return new AdminPaymentPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private AdminPaymentResponse toResponse(Payment payment) {
        return new AdminPaymentResponse(payment.getId(), payment.getOrganization().getName(),
                payment.getPlan().getName(), payment.getAmountPaise(), payment.getCurrency(),
                payment.getStatus(), payment.getCreatedAt());
    }
}