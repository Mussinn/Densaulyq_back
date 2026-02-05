package com.example.MedSafe.controller;

import com.example.MedSafe.model.Prescription;
import com.example.MedSafe.model.dto.PrescriptionRequest;
import com.example.MedSafe.repository.PrescriptionRepository;
import com.example.MedSafe.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/prescription")
@RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionService prescriptionService;

    @GetMapping("{diagnosesId}")
    public List<Prescription> findByDiagnosisId(@PathVariable Integer diagnosesId) {
        return prescriptionService.findByDiagnosisId(diagnosesId);
    }


    @GetMapping("{appointmentId}")
    public List<Prescription> findByAppointmentId(@PathVariable Integer appointmentId) {
        return prescriptionService.findByAppointmentId(appointmentId);
    }

    @PostMapping
    public Prescription save(@RequestBody PrescriptionRequest request) {
        return prescriptionService.save(request);
    }
}
