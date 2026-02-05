package com.example.MedSafe.controller;

import com.example.MedSafe.model.MedicalRecord;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.MedicalRecordRepository;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Patient>> getPatientByDoctorId() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @GetMapping("/medical-record/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getMedicalRecordByPatientId(@PathVariable Integer patientId) {
        return ResponseEntity.ok(medicalRecordRepository.findByPatientPatientId(patientId));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Patient> getCurrentPatient() {
        log.info("getCurrentPatient called");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("findByUsername username");
        User user = userService.findByUsername(username);
        log.info("findById called");
        Patient patient = patientRepository.findByUserUserId(user.getUserId()).orElseThrow(() -> new RuntimeException("Patient not found"));
        return ResponseEntity.ok(patient);
    }

}
