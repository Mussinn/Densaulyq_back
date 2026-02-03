package com.example.MedSafe.repository;

import com.example.MedSafe.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Поиск встреч по доктору
    List<Meeting> findByDoctorId(Long doctorId);

    // Поиск встреч по пациенту
    List<Meeting> findByPatientId(Long patientId);

    // Поиск встречи по roomId
    Optional<Meeting> findByRoomId(String roomId);

    // Поиск встреч по статусу и доктору
    List<Meeting> findByDoctorIdAndStatus(Long doctorId, String status);

    // Поиск встреч по записи на прием
    Optional<Meeting> findByAppointmentId(Long appointmentId);

    // Поиск предстоящих встреч для доктора
    @Query("SELECT m FROM Meeting m WHERE m.doctorId = :doctorId AND m.scheduledTime > :now AND m.status = 'SCHEDULED' ORDER BY m.scheduledTime ASC")
    List<Meeting> findUpcomingByDoctorId(@Param("doctorId") Long doctorId, @Param("now") LocalDateTime now);

    // Поиск активных встреч
    @Query("SELECT m FROM Meeting m WHERE m.status = 'ACTIVE' AND m.doctorId = :doctorId")
    List<Meeting> findActiveByDoctorId(@Param("doctorId") Long doctorId);

    // Поиск встреч по email пациента
    List<Meeting> findByPatientEmail(String patientEmail);

    // Поиск встреч по диапазону дат
    @Query("SELECT m FROM Meeting m WHERE m.doctorId = :doctorId AND m.scheduledTime BETWEEN :start AND :end ORDER BY m.scheduledTime ASC")
    List<Meeting> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}