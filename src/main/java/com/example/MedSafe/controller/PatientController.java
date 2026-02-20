package com.example.MedSafe.controller;

import com.example.MedSafe.model.MedicalRecord;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.PatientUpdateRequest;
import com.example.MedSafe.repository.MedicalRecordRepository;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PutMapping("/update-by-user/{userId}")
    public ResponseEntity<?> updatePatientByUserId(@PathVariable Integer userId, @RequestBody PatientUpdateRequest request) {
        try {
            // Найти пациента по userId
            Patient patient = patientRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Patient not found for user ID: " + userId));

            // Обновить поля
            if (request.getDateOfBirth() != null) {
                patient.setDateOfBirth(request.getDateOfBirth());
            }
            if (request.getGender() != null && !request.getGender().equals("male")) {
                patient.setGender(request.getGender());
            }
            if (request.getContactNumber() != null && !request.getContactNumber().equals("TEMP")) {
                patient.setContactNumber(request.getContactNumber());
            }
            if (request.getAddress() != null && !request.getAddress().equals("TEMP0")) {
                patient.setAddress(request.getAddress());
            }

            Patient updatedPatient = patientRepository.save(patient);
            return ResponseEntity.ok(updatedPatient);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
