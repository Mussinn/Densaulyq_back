package com.example.MedSafe.model.dto;
import lombok.Data;

@Data
public class PrescriptionRequest {
    private Integer diagnosisId;
    private Integer appointmentId;
    private String callback;
}
