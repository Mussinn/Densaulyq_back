package com.example.MedSafe.repository;

import com.example.MedSafe.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor,Integer> {

    Optional<Doctor> findByUserUserId(Integer userId);
}
