package com.verbamind.payment.controller;

import com.verbamind.common.dto.ApiResponse;
import com.verbamind.payment.dto.*;
import com.verbamind.payment.service.PaymentService;
import com.verbamind.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateOrderRequest request) {
        var order = paymentService.createOrder(currentUser.getId(), organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order created"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @Valid @RequestBody VerifyPaymentRequest request) {
        var payment = paymentService.verifyPayment(currentUser.getId(), organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(payment, "Payment verified, plan activated"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PaymentPageResponse>> history(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.listBillingHistory(currentUser.getId(), organizationId, pageable)));
    }
}