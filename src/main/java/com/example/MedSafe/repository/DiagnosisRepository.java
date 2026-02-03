package com.example.MedSafe.repository;

import com.example.MedSafe.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Integer> {
    List<Diagnosis> findByMedicalRecord_Patient_PatientId(Integer patientId);

    List<Diagnosis> findTop5ByOrderByCreatedAtDesc();

    @Query(value = "SELECT d.* FROM diagnoses d " +
            "JOIN medical_records mr ON d.record_id = mr.record_id " +
            "JOIN patients p ON mr.patient_id = p.patient_id " +
            "JOIN users u ON p.user_id = u.user_id " +
            "WHERE u.user_id = :userId",
            nativeQuery = true)
    List<Diagnosis> findByUserId(@Param("userId") Integer userId);

}
