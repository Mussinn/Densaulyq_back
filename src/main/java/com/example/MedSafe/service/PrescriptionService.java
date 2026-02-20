package com.example.MedSafe.service;

import com.example.MedSafe.model.Prescription;
import com.example.MedSafe.model.dto.PrescriptionRequest;
import com.example.MedSafe.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentService appointmentService;
    private final DiagnosisService diagnosisService;

    public List<Prescription> findByAppointmentId(Integer id) {
        return prescriptionRepository.findByAppointmentAppointmentId(id);
    }

    public List<Prescription> findByDiagnosisId(Integer id) {
        return prescriptionRepository.findByDiagnosisDiagnosisId(id);
    }

    public Prescription save(PrescriptionRequest request) {
        if (request.getDiagnosisId() == null) {
            throw new IllegalArgumentException("DiagnosisId is required");
        }
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("AppointmentId is required");
        }

        Prescription newPrescription = new Prescription();
        newPrescription.setAppointment(appointmentService.findById(request.getAppointmentId()));
        newPrescription.setDiagnosis(diagnosisService.getDiagnosisById(request.getDiagnosisId()));
        newPrescription.setCallback(request.getCallback());
        return prescriptionRepository.save(newPrescription);
    }

    public List<Prescription> findByDiagnosisMedicalRecordPatientPatientId(Integer id) {
        log.info("findByDiagnosisMedicalRecordPatientPatientId:{}", id);
        return prescriptionRepository.findByDiagnosisMedicalRecordPatientPatientId(id);
    }
}
