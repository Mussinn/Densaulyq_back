package com.example.MedSafe.controller;

import com.example.MedSafe.model.MedicalRecord;
import com.example.MedSafe.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-card")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordRepository medicalRecordRepository;

    @GetMapping
    public List<MedicalRecord> findByMedicalRecordId(Integer patientId) {
        return medicalRecordRepository.findByPatientPatientId(patientId);
    }
}
