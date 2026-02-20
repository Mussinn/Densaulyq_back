package com.example.MedSafe.repository;

import com.example.MedSafe.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Find payment by appointment
    Optional<Payment> findByAppointmentId(Integer appointmentId);

    // Find payment by Stripe PaymentIntent ID
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    // Find payment by Stripe Session ID
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

//    Optional<Payment> findByAppointmentPatientIdOrderByCreatedAtDesc(Integer patientId);
//
//    Optional<Payment> findByAppointmentDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    Optional<Payment> findByAppointmentPatientPatientId(Integer patientId);

    @Query("SELECT p FROM Payment p WHERE p.appointment.patient.patientId = :patientId")
    List<Payment> findByPatientId(@Param("patientId") Integer patientId);

    Optional<Payment> findByAppointmentDoctorDoctorIdOrderByCreatedAtDesc(Integer doctorId);

    Optional<Payment> findByAppointmentDoctorDoctorId(Integer doctorId);

//    Optional<Payment> findByPatientIdOrderByCreatedAtDesc(Integer patientId);
//
//    Optional<Payment> findByDoctorIdOrderByCreatedAtDesc(Integer doctorId);
}