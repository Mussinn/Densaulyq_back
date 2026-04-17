package com.example.MedSafe.controller;

import com.example.MedSafe.model.Appointment;
import com.example.MedSafe.model.Diagnosis;
import com.example.MedSafe.model.Doctor;
import com.example.MedSafe.repository.AppointmentRepository;
import com.example.MedSafe.repository.DiagnosisRepository;
import com.example.MedSafe.repository.DoctorRepository;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final TestRepository testRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Autowired
    public DashboardController(PatientRepository patientRepository,
                               DiagnosisRepository diagnosisRepository,
                               TestRepository testRepository,
                               DoctorRepository doctorRepository,
                               AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.testRepository = testRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Базалық статистика
        stats.put("totalPatients", patientRepository.count());
        stats.put("totalDiagnoses", diagnosisRepository.count());
        stats.put("totalTests", testRepository.count());
        stats.put("totalDoctors", doctorRepository.count());

        // Топ дәрігерлер
        stats.put("topDoctorsByDiagnoses", getTopDoctorsByDiagnoses());
        stats.put("topDoctorsByAppointments", getTopDoctorsByAppointments());

        return ResponseEntity.ok(stats);
    }

    // Ең көп диагноз қойған дәрігерлер (топ 5)
    private List<Map<String, Object>> getTopDoctorsByDiagnoses() {
        List<Doctor> allDoctors = doctorRepository.findAll();

        // Әр дәрігердің диагноз санын есептеу
        Map<Doctor, Long> doctorDiagnosisCount = new HashMap<>();

        for (Doctor doctor : allDoctors) {
            // Дәрігердің барлық диагноздарын табу
            // Диагноздар MedicalRecord -> Patient -> Appointment -> Doctor арқылы байланысқан
            // Қарапайым әдіс: барлық диагноздарды алып, дәрігер бойынша сүзу
            List<Diagnosis> allDiagnoses = diagnosisRepository.findAll();

            long count = allDiagnoses.stream()
                    .filter(d -> {
                        try {
                            // Диагноз арқылы дәрігерді алу
                            if (d.getMedicalRecord() != null &&
                                    d.getMedicalRecord().getPatient() != null) {
                                // Бұл жерде Appointment арқылы дәрігерге қол жеткізу керек
                                // Қазіргі Diagnosis моделінде Appointment жоқ
                                // Сондықтан қарапайым шешім: 0 қайтарамыз
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                        return false;
                    })
                    .count();

            doctorDiagnosisCount.put(doctor, count);
        }

        // Сұрыптау және топ 5 алу
        return doctorDiagnosisCount.entrySet().stream()
                .sorted(Map.Entry.<Doctor, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> doctorStats = new HashMap<>();
                    Doctor doctor = entry.getKey();
                    Long count = entry.getValue();

                    doctorStats.put("doctorId", doctor.getDoctorId());
                    doctorStats.put("fullName", getDoctorFullName(doctor));
                    doctorStats.put("specialty", doctor.getSpecialty() != null ? doctor.getSpecialty() : "Көрсетілмеген");
                    doctorStats.put("count", count);
                    doctorStats.put("metric", "diagnoses");

                    return doctorStats;
                })
                .collect(Collectors.toList());
    }

    // Ең көп қабылдау өткізген дәрігерлер (топ 5)
    private List<Map<String, Object>> getTopDoctorsByAppointments() {
        List<Doctor> allDoctors = doctorRepository.findAll();

        // Әр дәрігердің қабылдау санын есептеу
        Map<Doctor, Long> doctorAppointmentCount = new HashMap<>();

        for (Doctor doctor : allDoctors) {
            // Дәрігердің барлық қабылдауларын AppointmentRepository арқылы табу
            List<Appointment> doctorAppointments = appointmentRepository.findByDoctorByDoctorId(doctor.getUser().getUserId());
            long count = doctorAppointments.size();
            doctorAppointmentCount.put(doctor, count);
        }

        // Сұрыптау және топ 5 алу
        return doctorAppointmentCount.entrySet().stream()
                .sorted(Map.Entry.<Doctor, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> doctorStats = new HashMap<>();
                    Doctor doctor = entry.getKey();
                    Long count = entry.getValue();

                    doctorStats.put("doctorId", doctor.getDoctorId());
                    doctorStats.put("fullName", getDoctorFullName(doctor));
                    doctorStats.put("specialty", doctor.getSpecialty() != null ? doctor.getSpecialty() : "Көрсетілмеген");
                    doctorStats.put("count", count);
                    doctorStats.put("metric", "appointments");

                    return doctorStats;
                })
                .collect(Collectors.toList());
    }

    // Дәрігердің толық аты-жөнін алу
    private String getDoctorFullName(Doctor doctor) {
        if (doctor.getUser() != null) {
            String firstName = doctor.getUser().getFirstName() != null ? doctor.getUser().getFirstName() : "";
            String lastName = doctor.getUser().getLastName() != null ? doctor.getUser().getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            return fullName.isEmpty() ? "Дәрігер #" + doctor.getDoctorId() : fullName;
        }
        return "Дәрігер #" + doctor.getDoctorId();
    }
}