package com.example.MedSafe.repository;

import com.example.MedSafe.model.MeetingConsilium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingConsiliumRepository extends JpaRepository<MeetingConsilium, Integer> {
    List<MeetingConsilium> findBySenderDoctorId(Integer doctorId);

    List<MeetingConsilium> findByReceiverDoctorId(Integer doctorId);

    List<MeetingConsilium> findActiveBySenderDoctorId(Integer doctorId);

    List<MeetingConsilium> findBySenderDoctorIdAndScheduledTimeAfter(Integer doctorId, LocalDateTime now);

    Optional<MeetingConsilium> findByRoomId(String roomId);
}
