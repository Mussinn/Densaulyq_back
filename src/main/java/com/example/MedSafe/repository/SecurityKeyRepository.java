package com.example.MedSafe.repository;

import com.example.MedSafe.model.SecurityKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SecurityKeyRepository extends JpaRepository<SecurityKey, Integer> {
    Optional<SecurityKey> findByUserUserId(Integer userId);
}
