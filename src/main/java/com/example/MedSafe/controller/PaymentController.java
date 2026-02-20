package com.example.MedSafe.controller;

import com.example.MedSafe.model.dto.CreatePaymentRequest;
import com.example.MedSafe.model.dto.PaymentResponse;
import com.example.MedSafe.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret:whsec_example}")
    private String stripeWebhookSecret;

    /**
     * Doctor creates payment after completing appointment
     * POST /api/payments/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        try {
            // Get doctor ID from authentication
            Integer doctorId = getDoctorIdFromAuth(authentication);

            PaymentResponse payment = paymentService.createPaymentByDoctor(doctorId, request);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get payment by appointment ID
     * GET /api/payments/appointment/{appointmentId}
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getPaymentByAppointment(@PathVariable Integer appointmentId) {
        return paymentService.getPaymentByAppointment(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all payments for current patient
     * GET /api/payments/patient/me
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@PathVariable Integer patientId) {
        List<PaymentResponse> payments = paymentService.getPatientPayments(patientId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments for current doctor
     * GET /api/payments/doctor/me
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<PaymentResponse>> getMyDoctorPayments(@PathVariable Integer doctorId) {
        List<PaymentResponse> payments = paymentService.getDoctorPayments(doctorId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Stripe webhook endpoint
     * POST /api/payments/webhook/stripe
     *
     * This endpoint receives events from Stripe about payment status changes
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<?> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Verify webhook signature for security
            event = Webhook.constructEvent(
                    payload, sigHeader, stripeWebhookSecret
            );
        } catch (SignatureVerificationException e) {
            // Invalid signature - reject the webhook
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid signature"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }

        // Handle the event
        try {
            String eventType = event.getType();
            Map<String, Object> eventData = event.getDataObjectDeserializer()
                    .getObject()
                    .map(obj -> (Map<String, Object>) obj)
                    .orElseThrow(() -> new RuntimeException("Failed to deserialize event data"));

            paymentService.processStripeWebhook(eventType, eventData);

            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually mark payment as paid (for testing or cash payments)
     * POST /api/payments/{paymentId}/mark-paid
     */
    @PostMapping("/{paymentId}/mark-paid")
    public ResponseEntity<?> markAsPaid(
            @PathVariable Integer paymentId,
            Authentication authentication) {
        try {
            Integer doctorId = getDoctorIdFromAuth(authentication);
            PaymentResponse payment = paymentService.markAsPaid(paymentId, doctorId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Payment success callback (for frontend redirect)
     * GET /api/payments/success
     */
    @GetMapping("/success")
    public ResponseEntity<?> paymentSuccess(@RequestParam String session_id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Платеж успешно завершен",
                "sessionId", session_id
        ));
    }

    /**
     * Payment cancel callback (for frontend redirect)
     * GET /api/payments/cancel
     */
    @GetMapping("/cancel")
    public ResponseEntity<?> paymentCancel() {
        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Платеж был отменен"
        ));
    }

    // Helper methods to extract user info from authentication
    private Integer getDoctorIdFromAuth(Authentication authentication) {
        // Implement based on your security configuration
        // This is a placeholder - adjust according to your User/Doctor model
        // Example:
        // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // return userDetails.getDoctor().getDoctorId();
        return 1; // Replace with actual logic
    }

    private Integer getPatientIdFromAuth(Authentication authentication) {
        // Implement based on your security configuration
        // CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        // return userDetails.getPatient().getPatientId();
        return 1; // Replace with actual logic
    }
}