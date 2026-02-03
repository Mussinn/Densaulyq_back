package com.example.MedSafe.controller;

import com.example.MedSafe.model.Doctor;
import com.example.MedSafe.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctor")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorRepository doctorRepository;

    @GetMapping
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }
}
