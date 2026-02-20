package com.example.MedSafe.controller;

import com.example.MedSafe.model.Doctor;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.DoctorUpdateRequest;
import com.example.MedSafe.repository.DoctorRepository;
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
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {
    private final DoctorRepository doctorRepository;
    private final UserService userService;

    @GetMapping
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @PutMapping("/update-by-user/{userId}")
    public ResponseEntity<?> updateDoctorByUserId(@PathVariable Integer userId, @RequestBody DoctorUpdateRequest request) {
        try {
            // Найти доктора по userId
            Doctor doctor = doctorRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found for user ID: " + userId));

            // Обновить поля
            if (request.getSpecialty() != null && !request.getSpecialty().equals("TEMP")) {
                doctor.setSpecialty(request.getSpecialty());
            }
            if (request.getContactNumber() != null && !request.getContactNumber().equals("TEMP")) {
                doctor.setContactNumber(request.getContactNumber());
            }

            Doctor updatedDoctor = doctorRepository.save(doctor);
            return ResponseEntity.ok(updatedDoctor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Doctor> getCurrentDoctor() {
        log.info("getCurrentDoctor called");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("findByUsername username");
        User user = userService.findByUsername(username);
        log.info("findById called");
        Doctor doctor = doctorRepository.findByUserUserId(user.getUserId()).orElseThrow(() -> new RuntimeException("Patient not found"));
        return ResponseEntity.ok(doctor);
    }
}
