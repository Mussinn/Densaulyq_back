package com.example.MedSafe.service;

import com.example.MedSafe.model.Appointment;
import com.example.MedSafe.model.Doctor;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.dto.AppointmentRequestDTO;
import com.example.MedSafe.repository.AppointmentRepository;
import com.example.MedSafe.repository.DoctorRepository;
import com.example.MedSafe.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public Appointment findById(Integer id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new RuntimeException("appointment not found"));
    }

    public List<Appointment> findByPatientId(Integer patientId) {
        return appointmentRepository.findByPatientByPatientId(patientId);
    }

    public List<Appointment> findByDoctorId(Integer doctorId) {
        return appointmentRepository.findByDoctorByDoctorId(doctorId);
    }

    public Appointment create(AppointmentRequestDTO appointmentRequestDTO) {
        log.info("if setStatus");
        if (appointmentRequestDTO.getStatus() == null) {
            appointmentRequestDTO.setStatus("scheduled");
        }

        log.info("getPatientId = " + appointmentRequestDTO.getPatientId());
        log.info("getDoctorId = " + appointmentRequestDTO.getDoctorId());

        // Логируем перед поиском пациента
        log.info("Finding patient with id: {}", appointmentRequestDTO.getPatientId());
        Patient patient = patientRepository.findById(appointmentRequestDTO.getPatientId())
                .orElseThrow(() -> {
                    log.error("Patient not found with id: {}", appointmentRequestDTO.getPatientId());
                    return new RuntimeException("Patient not found");
                });
        log.info("Patient found: {}", patient.getPatientId());

        // Логируем перед поиском доктора
        log.info("Finding doctor with id: {}", appointmentRequestDTO.getDoctorId());
        Doctor doctor = doctorRepository.findById(appointmentRequestDTO.getDoctorId())
                .orElseThrow(() -> {
                    log.error("Doctor not found with id: {}", appointmentRequestDTO.getDoctorId());
                    return new RuntimeException("Doctor not found");
                });
        log.info("Doctor found: {}", doctor.getDoctorId());

        log.info("Creating new Appointment object");
        Appointment newAppointment = new Appointment(
                patient,
                doctor,
                appointmentRequestDTO.getAppointmentDate(),
                appointmentRequestDTO.getStatus()
        );

        log.info("Saving appointment to database");
        Appointment savedAppointment = appointmentRepository.save(newAppointment);
        log.info("Appointment saved with id: {}", savedAppointment.getAppointmentId());

        return savedAppointment;
    }

    public Appointment update(Long id, Appointment updatedAppointment) {
        return appointmentRepository.findById(Math.toIntExact(id))
                .map(existingAppointment -> {
                    // Обновляем только разрешенные поля
                    if (updatedAppointment.getAppointmentDate() != null) {
                        existingAppointment.setAppointmentDate(updatedAppointment.getAppointmentDate());
                    }
                    return appointmentRepository.save(existingAppointment);
                })
                .orElseThrow(() -> new RuntimeException("Қабылдау табылмады id: " + id));
    }

    public Appointment updateStatus(Long id, String status) {
        return appointmentRepository.findById(Math.toIntExact(id))
                .map(appointment -> {
                    appointment.setStatus(status);
                    return appointmentRepository.save(appointment);
                })
                .orElseThrow(() -> new RuntimeException("Қабылдау табылмады id: " + id));
    }

    @Transactional
    public boolean delete(Long id) {
        if (appointmentRepository.existsById(Math.toIntExact(id))) {
            appointmentRepository.deleteById(Math.toIntExact(id));
            return true;
        }
        return false;
    }

    @Transactional
    public Appointment softDelete(Long id) {
        return appointmentRepository.findById(Math.toIntExact(id))
                .map(appointment -> {
                    appointment.setStatus("CANCELLED");
                    return appointmentRepository.save(appointment);
                })
                .orElseThrow(() -> new RuntimeException("Қабылдау табылмады id: " + id));
    }

    public List<Appointment> findUpcomingAppointments(LocalDateTime fromDate) {
        return appointmentRepository.findByAppointmentDateAfterAndStatusNot(
                fromDate, "CANCELLED"
        );
    }

    public List<Appointment> findByDate(String date) {
        return appointmentRepository.findByAppointmentDate(date);
    }

    public boolean isTimeSlotAvailable(Integer doctorId, String date, String time) {
        List<Appointment> existingAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        doctorId, date, time, "CANCELLED"
                );
        return existingAppointments.isEmpty();
    }

    public AppointmentStatistics getStatistics() {
        long total = appointmentRepository.count();
        long scheduled = appointmentRepository.countByStatus("SCHEDULED");
        long completed = appointmentRepository.countByStatus("COMPLETED");
        long cancelled = appointmentRepository.countByStatus("CANCELLED");

        return new AppointmentStatistics(total, scheduled, completed, cancelled);
    }

    public static class AppointmentStatistics {
        private final long totalAppointments;
        private final long scheduledAppointments;
        private final long completedAppointments;
        private final long cancelledAppointments;

        public AppointmentStatistics(long total, long scheduled, long completed, long cancelled) {
            this.totalAppointments = total;
            this.scheduledAppointments = scheduled;
            this.completedAppointments = completed;
            this.cancelledAppointments = cancelled;
        }

        public long getTotalAppointments() { return totalAppointments; }
        public long getScheduledAppointments() { return scheduledAppointments; }
        public long getCompletedAppointments() { return completedAppointments; }
        public long getCancelledAppointments() { return cancelledAppointments; }
    }
}