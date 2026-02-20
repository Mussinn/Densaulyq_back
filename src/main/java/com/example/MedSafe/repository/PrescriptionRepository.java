package com.example.MedSafe.repository;

import com.example.MedSafe.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findByAppointmentAppointmentId(Integer id);

    List<Prescription> findByDiagnosisDiagnosisId(Integer id);

    List<Prescription> findByDiagnosisMedicalRecordPatientPatientId(Integer PatientId);
}
