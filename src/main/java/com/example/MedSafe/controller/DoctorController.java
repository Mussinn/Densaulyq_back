package com.example.MedSafe.controller;

import com.example.MedSafe.model.Doctor;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.DoctorRepository;
import com.example.MedSafe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
