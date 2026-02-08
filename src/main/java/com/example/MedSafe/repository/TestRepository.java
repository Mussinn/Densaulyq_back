package com.example.MedSafe.repository;

import com.example.MedSafe.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Integer> {
    List<Test> findByPatientPatientId(Integer patientId);
}
