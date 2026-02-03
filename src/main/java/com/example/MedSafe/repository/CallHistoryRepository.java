package com.example.MedSafe.repository;

import com.example.MedSafe.model.CallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallHistoryRepository extends JpaRepository<CallHistory, Integer> {
}
