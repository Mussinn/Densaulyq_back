package com.example.MedSafe.model.dto;

import com.example.MedSafe.model.Diagnosis;
import com.example.MedSafe.model.Prescription;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicalDataResponse {
    private Integer recordId;
    private Integer patientId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private List<Prescription> prescriptionList;
}
