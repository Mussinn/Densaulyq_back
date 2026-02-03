package com.example.MedSafe.repository;

import com.example.MedSafe.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // 1. Найти записи по ID пациента
    @Query("SELECT a FROM Appointment a WHERE a.patient.patientId = :patientId")
    List<Appointment> findByPatientByPatientId(@Param("patientId") Integer patientId);

    // 2. Найти записи по ID врача
    @Query("SELECT a FROM Appointment a WHERE a.doctor.user.userId = :doctorId")
    List<Appointment> findByDoctorByDoctorId(@Param("doctorId") Integer doctorId);

    // 3. Найти предстоящие записи после указанной даты и не отмененные
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate > :fromDate AND a.status != :cancelled")
    List<Appointment> findByAppointmentDateAfterAndStatusNot(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("cancelled") String cancelled
    );

    // 4. Найти записи по дате (формат YYYY-MM-DD)
    @Query("SELECT a FROM Appointment a WHERE DATE(a.appointmentDate) = :date")
    List<Appointment> findByAppointmentDate(@Param("date") String date);

    // 5. Проверить доступность времени у врача
    @Query("SELECT a FROM Appointment a WHERE a.doctor.doctorId = :doctorId " +
            "AND DATE(a.appointmentDate) = :date " +
            "AND FUNCTION('HOUR', a.appointmentDate) = :hour " +
            "AND FUNCTION('MINUTE', a.appointmentDate) = :minute " +
            "AND a.status != :cancelled")
    List<Appointment> findByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            @Param("doctorId") Integer doctorId,
            @Param("date") String date,
            @Param("time") String time,
            @Param("cancelled") String cancelled
    );

    // 6. Подсчитать записи по статусу
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :scheduled")
    long countByStatus(@Param("scheduled") String scheduled);
}