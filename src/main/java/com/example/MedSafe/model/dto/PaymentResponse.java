package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO для ответа с информацией о платеже
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Integer paymentId;
    private Integer appointmentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentUrl;
    private String invoiceId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    // Appointment info
    private String patientName;
    private String doctorName;
    private LocalDateTime appointmentDate;
}
