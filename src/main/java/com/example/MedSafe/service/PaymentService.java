package com.example.MedSafe.service;

import com.example.MedSafe.model.Appointment;
import com.example.MedSafe.model.Payment;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.CreatePaymentRequest;
import com.example.MedSafe.model.dto.PaymentResponse;
import com.example.MedSafe.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${stripe.api.key:sk_test_example}")
    private String stripeApiKey;

    @Value("${app.base.url:http://localhost:3000}")
    private String appBaseUrl;

    /**
     * Doctor creates payment after completing appointment
     */
    @Transactional
    public PaymentResponse createPaymentByDoctor(Integer doctorId, CreatePaymentRequest request) {
        // Validate appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));

        // Check if appointment belongs to this doctor
        if (!appointment.getDoctor().getDoctorId().equals(doctorId)) {
            throw new RuntimeException("У вас нет доступа к этой записи");
        }

        // Check if appointment is completed
        if (!"COMPLETED".equalsIgnoreCase(appointment.getStatus())) {
            throw new RuntimeException("Запись должна быть завершена перед выставлением счета");
        }

        // Check if payment already exists
        Optional<Payment> existingPayment = paymentRepository.findByAppointmentId(request.getAppointmentId());
        if (existingPayment.isPresent()) {
            throw new RuntimeException("Счет уже выставлен для этой записи");
        }

        // Create payment
        Payment payment = new Payment();
        payment.setAppointmentId(request.getAppointmentId());
        payment.setAmount(request.getAmount());
        payment.setCurrency("kzt"); // Stripe requires USD for Kazakhstan
        payment.setStatus("pending");

        // Create Stripe Checkout Session
        try {
            String checkoutUrl = createStripeCheckoutSession(payment, appointment);
            payment.setPaymentUrl(checkoutUrl);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания платежной сессии Stripe: " + e.getMessage());
        }

        Payment savedPayment = paymentRepository.save(payment);

        return mapToPaymentResponse(savedPayment);
    }

    /**
     * Create Stripe Checkout Session
     */
    private String createStripeCheckoutSession(Payment payment, Appointment appointment) throws StripeException {
        // Set Stripe API key
        Stripe.apiKey = stripeApiKey;

        String patientEmail = appointment.getPatient().getUser().getEmail();

        // Convert amount to cents (Stripe works with smallest currency unit)
        // For USD: 50.00 USD = 5000 cents
        long amountInCents = payment.getAmount().multiply(new BigDecimal(100)).longValue();

        // Create Checkout Session parameters
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appBaseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appBaseUrl + "/payment/cancel")
                .setCustomerEmail(patientEmail)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(payment.getCurrency())
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Медицинская консультация")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("appointment_id", String.valueOf(payment.getAppointmentId()))
                .build();

        // Create the session
        Session session = Session.create(params);

        // Save session ID to payment
        payment.setStripeSessionId(session.getId());
        payment.setStripePaymentIntentId(session.getPaymentIntent());

        // Return the checkout URL
        return session.getUrl();
    }

    /**
     * Get payment by appointment ID
     */
    public Optional<PaymentResponse> getPaymentByAppointment(Integer appointmentId) {
        return paymentRepository.findByAppointmentId(appointmentId)
                .map(this::mapToPaymentResponse);
    }

    /**
     * Get all payments for patient
     */
    public List<PaymentResponse> getPatientPayments(Integer patientId) {
        return paymentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for doctor
     */
    public List<PaymentResponse> getDoctorPayments(Integer doctorId) {
        return paymentRepository.findByAppointmentDoctorDoctorId(doctorId)
                .stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     *
     *
     * Process Stripe webhook event
     */
    @Transactional
    public void processStripeWebhook(String eventType, Map<String, Object> eventData) {
        if ("checkout.session.completed".equals(eventType)) {
            handleCheckoutSessionCompleted(eventData);
        } else if ("payment_intent.succeeded".equals(eventType)) {
            handlePaymentIntentSucceeded(eventData);
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            handlePaymentIntentFailed(eventData);
        }
    }

    /**
     * Handle successful checkout session
     */
    private void handleCheckoutSessionCompleted(Map<String, Object> sessionData) {
        String sessionId = (String) sessionData.get("id");

        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));

        // Extract metadata
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) sessionData.get("metadata");

        // Update payment status
        payment.setStatus("paid");
        payment.setPaidAt(LocalDateTime.now());
        payment.setStripePaymentIntentId((String) sessionData.get("payment_intent"));

        paymentRepository.save(payment);
    }

    /**
     * Handle successful payment intent
     */
    private void handlePaymentIntentSucceeded(Map<String, Object> paymentIntentData) {
        String paymentIntentId = (String) paymentIntentData.get("id");

        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus("paid");
            payment.setPaidAt(LocalDateTime.now());

            // Extract payment method details if available
            @SuppressWarnings("unchecked")
            Map<String, Object> charges = (Map<String, Object>) paymentIntentData.get("charges");
            if (charges != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> chargesList = (List<Map<String, Object>>) charges.get("data");
                if (chargesList != null && !chargesList.isEmpty()) {
                    Map<String, Object> charge = chargesList.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paymentMethodDetails = (Map<String, Object>) charge.get("payment_method_details");
                    if (paymentMethodDetails != null) {
                        String type = (String) paymentMethodDetails.get("type");
                        payment.setPaymentMethod(type);
                    }
                }
            }

            paymentRepository.save(payment);
        });
    }

    /**
     * Handle failed payment intent
     */
    private void handlePaymentIntentFailed(Map<String, Object> paymentIntentData) {
        String paymentIntentId = (String) paymentIntentData.get("id");

        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus("failed");
            paymentRepository.save(payment);
        });
    }

    /**
     * Manually mark payment as paid (for testing or cash payments)
     */
    @Transactional
    public PaymentResponse markAsPaid(Integer paymentId, Integer doctorId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));

        payment.setStatus("paid");
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentMethod("manual");
        payment.setTransactionId("MANUAL-" + System.currentTimeMillis());

        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(savedPayment);
    }

    /**
     * Map Payment entity to PaymentResponse DTO
     */
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setAppointmentId(payment.getAppointmentId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setPaymentUrl(payment.getPaymentUrl());
        response.setCreatedAt(payment.getCreatedAt());
        response.setPaidAt(payment.getPaidAt());

        // Add appointment details if available
        if (payment.getAppointment() != null) {
            Appointment apt = payment.getAppointment();
            response.setAppointmentDate(apt.getAppointmentDate());

            if (apt.getPatient() != null && apt.getPatient().getUser() != null) {
                User patient = apt.getPatient().getUser();
                response.setPatientName(patient.getFirstName() + " " + patient.getLastName());
            }

            if (apt.getDoctor() != null && apt.getDoctor().getUser() != null) {
                User doctor = apt.getDoctor().getUser();
                response.setDoctorName(doctor.getFirstName() + " " + doctor.getLastName());
            }
        }

        return response;
    }
}