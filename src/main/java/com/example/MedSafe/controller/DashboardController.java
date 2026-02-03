package com.example.MedSafe.controller;

import com.example.MedSafe.repository.DiagnosisRepository;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final TestRepository testRepository;

    @Autowired
    public DashboardController(PatientRepository patientRepository, DiagnosisRepository diagnosisRepository, TestRepository testRepository) {
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.testRepository = testRepository;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalDiagnoses", diagnosisRepository.count());
        stats.put("totalTests", testRepository.count());

        return ResponseEntity.ok(stats);
    }
}
