package com.example.MedSafe.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnoses")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer diagnosisId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Size(max = 1000)
    private String diagnosis;

    @Column(nullable = false)
    private LocalDate diagnosisDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Diagnosis(MedicalRecord medicalRecord, String diagnosis, LocalDate diagnosisDate) {
        this.medicalRecord = medicalRecord;
        this.diagnosis = diagnosis;
        this.diagnosisDate = diagnosisDate;
    }
}
