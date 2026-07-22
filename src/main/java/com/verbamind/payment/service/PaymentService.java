package com.verbamind.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.verbamind.auth.service.EmailService;
import com.verbamind.organization.entity.OrganizationRole;
import com.verbamind.document.service.OrganizationAccessGuard;
import com.verbamind.payment.dto.*;
import com.verbamind.payment.entity.Payment;
import com.verbamind.payment.entity.PaymentStatus;
import com.verbamind.payment.exception.InvalidPlanForPaymentException;
import com.verbamind.payment.exception.PaymentNotFoundException;
import com.verbamind.payment.exception.PaymentVerificationException;
import com.verbamind.payment.repository.PaymentRepository;
import com.verbamind.payment.config.RazorpayProperties;
import com.verbamind.subscription.entity.Plan;
import com.verbamind.subscription.entity.PlanCode;
import com.verbamind.subscription.repository.PlanRepository;
import com.verbamind.subscription.service.SubscriptionService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final OrganizationAccessGuard accessGuard;
    private final SubscriptionService subscriptionService;
    private final com.verbamind.organization.repository.OrganizationRepository organizationRepository;
    private final EmailService emailService;

    public PaymentService(PaymentRepository paymentRepository,
                          PlanRepository planRepository,
                          RazorpayClient razorpayClient,
                          RazorpayProperties razorpayProperties,
                          OrganizationAccessGuard accessGuard,
                          SubscriptionService subscriptionService,
                          com.verbamind.organization.repository.OrganizationRepository organizationRepository,
                          EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.razorpayClient = razorpayClient;
        this.razorpayProperties = razorpayProperties;
        this.accessGuard = accessGuard;
        this.subscriptionService = subscriptionService;
        this.organizationRepository = organizationRepository;
        this.emailService = emailService;
    }


    @Transactional
    public CreateOrderResponse createOrder(UUID currentUserId, UUID organizationId, CreateOrderRequest request) {
        accessGuard.requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        if (request.planCode() == PlanCode.FREE) {
            throw new InvalidPlanForPaymentException("FREE plan does not require payment");
        }

        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new InvalidPlanForPaymentException("Plan not found: " + request.planCode()));

        var org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new InvalidPlanForPaymentException("Organization not found"));

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", plan.getPriceMonthlyPaise());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "org_" + organizationId + "_" + System.currentTimeMillis());

            var razorpayOrder = razorpayClient.orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setOrganization(org);
            payment.setPlan(plan);
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setAmountPaise(plan.getPriceMonthlyPaise());
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.CREATED);
            paymentRepository.save(payment);

            return new CreateOrderResponse(
                    payment.getRazorpayOrderId(), razorpayProperties.getKeyId(),
                    plan.getPriceMonthlyPaise(), "INR");

        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new PaymentVerificationException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(UUID currentUserId, UUID organizationId, VerifyPaymentRequest request) {
        accessGuard.requireRole(organizationId, currentUserId, OrganizationRole.ADMIN);

        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment order not found"));

        if (!payment.getOrganization().getId().equals(organizationId)) {
            throw new PaymentNotFoundException("Payment order not found");
        }

        boolean valid = verifySignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentVerificationException("Payment signature verification failed");
        }

        markPaidAndActivate(payment, request.razorpayPaymentId(), request.razorpaySignature());
        return toPaymentResponse(payment);
    }

    @Transactional
    public void handleWebhook(String rawBody, String webhookSignatureHeader) {
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(rawBody, webhookSignatureHeader, razorpayProperties.getWebhookSecret());
        } catch (Exception e) {
            log.error("Webhook signature verification threw an exception: {}", e.getMessage());
            throw new PaymentVerificationException("Invalid webhook signature");
        }

        if (!valid) {
            throw new PaymentVerificationException("Invalid webhook signature");
        }

        JSONObject payload = new JSONObject(rawBody);
        String event = payload.optString("event");

        if (!"payment.captured".equals(event)) {
            log.info("Ignoring Razorpay webhook event: {}", event);
            return; // only act on successful captures for V1
        }

        JSONObject paymentEntity = payload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String orderId = paymentEntity.getString("order_id");
        String paymentId = paymentEntity.getString("id");

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook for unknown order_id: {}", orderId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return; // already processed via verifyPayment(); avoid double-activation
        }

        markPaidAndActivate(payment, paymentId, null);
    }

    public PaymentPageResponse listBillingHistory(UUID currentUserId, UUID organizationId, Pageable pageable) {
        accessGuard.requireMembership(organizationId, currentUserId);
        Page<Payment> page = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
        List<PaymentResponse> items = page.getContent().stream().map(this::toPaymentResponse).toList();
        return new PaymentPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private void markPaidAndActivate(Payment payment, String razorpayPaymentId, String razorpaySignature) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        if (razorpaySignature != null) payment.setRazorpaySignature(razorpaySignature);
        paymentRepository.save(payment);

        subscriptionService.changePlan(payment.getOrganization().getId(), payment.getPlan().getCode());

        emailService.sendPaymentSuccessEmail(
                payment.getOrganization().getOwner().getEmail(),
                payment.getPlan().getName()
        );
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);
            return Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret());
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getPlan().getName(),
                payment.getAmountPaise(), payment.getCurrency(), payment.getStatus(), payment.getCreatedAt());
    }
}