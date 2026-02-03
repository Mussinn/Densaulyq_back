package com.example.MedSafe.controller;

import com.example.MedSafe.model.MedicalRecord;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.repository.MedicalRecordRepository;
import com.example.MedSafe.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientController {
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @GetMapping
    public ResponseEntity<List<Patient>> getPatientByDoctorId() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @GetMapping("/medical-record/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordByPatientId(@PathVariable Integer patientId) {
        return ResponseEntity.ok(medicalRecordRepository.findByPatientPatientId(patientId));
    }
}
