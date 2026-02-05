package com.example.MedSafe.controller;

import com.example.MedSafe.model.Appointment;
import com.example.MedSafe.model.dto.AppointmentRequestDTO;
import com.example.MedSafe.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // 1. Получить все записи
    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        try {
            List<Appointment> appointments = appointmentService.findAll();
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 2. Получить запись по ID
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(appointmentService.findById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 3. Получить записи пациента
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatient(@PathVariable Integer patientId) {
        try {
            List<Appointment> appointments = appointmentService.findByPatientId(patientId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 4. Получить записи врача
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByDoctor(@PathVariable Integer doctorId) {
        try {
            List<Appointment> appointments = appointmentService.findByDoctorId(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 5. Получить предстоящие записи
    @GetMapping("/upcoming")
    public ResponseEntity<List<Appointment>> getUpcomingAppointments(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate) {
        try {
            LocalDateTime date = fromDate != null ? fromDate : LocalDateTime.now();
            List<Appointment> appointments = appointmentService.findUpcomingAppointments(date);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 6. Получить записи по дате (если нужно)
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Appointment>> getAppointmentsByDate(@PathVariable String date) {
        try {
            List<Appointment> appointments = appointmentService.findByDate(date);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 7. Создать новую запись
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody AppointmentRequestDTO appointment) {
        try {
            Appointment createdAppointment = appointmentService.create(appointment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 8. Обновить запись полностью
    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(
            @PathVariable Long id,
            @RequestBody Appointment appointment) {
        try {
            Appointment updatedAppointment = appointmentService.update(id, appointment);
            return ResponseEntity.ok(updatedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 9. Обновить статус записи
    @PatchMapping("/{id}/status")
    public ResponseEntity<Appointment> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            Appointment updatedAppointment = appointmentService.updateStatus(id, status);
            return ResponseEntity.ok(updatedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 10. Удалить запись (физическое удаление)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAppointment(@PathVariable Long id) {
        try {
            boolean deleted = appointmentService.delete(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Қабылдау сәтті жойылды",
                        "id", id
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Қабылдау табылмады",
                                "id", id
                        ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Жойу кезінде қате орын алды",
                            "error", e.getMessage()
                    ));
        }
    }

    // 11. Отменить запись (мягкое удаление)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable Long id) {
        try {
            Appointment cancelledAppointment = appointmentService.softDelete(id);
            return ResponseEntity.ok(cancelledAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 12. Проверить доступность времени у врача
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Integer doctorId,
            @RequestParam String date,
            @RequestParam String time) {
        try {
            boolean available = appointmentService.isTimeSlotAvailable(doctorId, date, time);
            return ResponseEntity.ok(Map.of(
                    "available", available,
                    "doctorId", doctorId,
                    "date", date,
                    "time", time
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "available", false,
                            "error", e.getMessage()
                    ));
        }
    }

    // 13. Получить статистику записей
    @GetMapping("/statistics")
    public ResponseEntity<AppointmentService.AppointmentStatistics> getStatistics() {
        try {
            AppointmentService.AppointmentStatistics statistics = appointmentService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 14. Подтвердить запись (специальный endpoint для подтверждения)
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable Long id) {
        try {
            Appointment confirmedAppointment = appointmentService.updateStatus(id, "CONFIRMED");
            return ResponseEntity.ok(confirmedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 15. Отметить запись как завершенную
    @PostMapping("/{id}/complete")
    public ResponseEntity<Appointment> completeAppointment(@PathVariable Long id) {
        try {
            Appointment completedAppointment = appointmentService.updateStatus(id, "COMPLETED");
            return ResponseEntity.ok(completedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}