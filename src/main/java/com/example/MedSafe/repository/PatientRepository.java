package com.example.MedSafe.repository;

import com.example.MedSafe.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {
    List<Patient> findAllByUserUserId(Integer docktorId);

    Optional<Patient> findByUserUserId(Integer userId);
}
