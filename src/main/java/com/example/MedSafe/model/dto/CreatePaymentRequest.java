package com.example.MedSafe.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

// DTO для создания платежа доктором
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    private Integer appointmentId;
    private BigDecimal amount;
    private String description;
}

