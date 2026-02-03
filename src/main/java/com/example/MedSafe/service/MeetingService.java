package com.example.MedSafe.service;

import com.example.MedSafe.model.Meeting;
import com.example.MedSafe.repository.MeetingRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MailService emailService; // Предполагается, что у вас есть сервис для отправки email

    // Создание новой встречи
    @Transactional
    public Meeting createMeeting(CreateMeetingRequest request) {
        String roomId = generateRoomId(request.getTopic());
        String meetingUrl = "https://meet.jit.si/" + roomId;

        Meeting meeting = new Meeting();
        meeting.setAppointmentId(request.getAppointmentId());
        meeting.setDoctorId(request.getDoctorId());
        meeting.setPatientId(request.getPatientId());
        meeting.setRoomId(roomId);
        meeting.setMeetingUrl(meetingUrl);
        meeting.setTopic(request.getTopic());
        meeting.setStatus("SCHEDULED");
        meeting.setScheduledTime(request.getScheduledTime() != null ? request.getScheduledTime() : LocalDateTime.now().plusHours(1));
        meeting.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30);


        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("Meeting created with ID: {}", savedMeeting.getId());

        // Отправка приглашения пациенту
        if (request.getPatientEmail() != null && !request.getPatientEmail().trim().isEmpty()) {
            sendInvitation(savedMeeting);
        }

        return savedMeeting;
    }

    // Генерация уникального roomId
    private String generateRoomId(String topic) {
        String baseId = UUID.randomUUID().toString().substring(0, 8);
        if (topic != null && !topic.trim().isEmpty()) {
            String sanitizedTopic = topic.toLowerCase()
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-");
            return sanitizedTopic + "-" + baseId;
        }
        return "meeting-" + baseId;
    }

    // Отправка приглашения
    private void sendInvitation(Meeting meeting) {
        try {
            String subject = "Приглашение на видеоконсультацию";
            String body = String.format(
                    """
                    Уважаемый пациент,
                    
                    Вам назначена видеоконсультация с доктором.
                    
                    Тема: %s
                    Дата: %s
                    Ссылка для подключения: %s
                    
                    С уважением,
                    Медицинская система MedSafe
                    """,
                    meeting.getTopic(),
                    meeting.getScheduledTime(),
                    meeting.getMeetingUrl()
            );

            emailService.send(meeting.getPatientEmail(), subject, body);
            meetingRepository.save(meeting);
            log.info("Invitation sent for meeting ID: {}", meeting.getId());
        } catch (Exception e) {
            log.error("Failed to send invitation for meeting ID: {}", meeting.getId(), e);
        }
    }

    // Получение всех встреч доктора
    public List<Meeting> getDoctorMeetings(Long doctorId) {
        return meetingRepository.findByDoctorId(doctorId);
    }

    // Получение активных встреч доктора
    public List<Meeting> getActiveMeetings(Long doctorId) {
        return meetingRepository.findActiveByDoctorId(doctorId);
    }

    // Получение предстоящих встреч
    public List<Meeting> getUpcomingMeetings(Long doctorId) {
        return meetingRepository.findUpcomingByDoctorId(doctorId, LocalDateTime.now());
    }

    // Поиск встречи по ID
    public Optional<Meeting> getMeetingById(Long id) {
        return meetingRepository.findById(id);
    }

    // Поиск встречи по roomId
    public Optional<Meeting> getMeetingByRoomId(String roomId) {
        return meetingRepository.findByRoomId(roomId);
    }

    // Обновление статуса встречи
    @Transactional
    public Meeting updateMeetingStatus(Long id, String status) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        String oldStatus = meeting.getStatus();
        meeting.setStatus(status);

        Meeting updated = meetingRepository.save(meeting);
        log.info("Meeting ID: {} status changed from {} to {}", id, oldStatus, status);

        return updated;
    }

    // Начать встречу
    @Transactional
    public Meeting startMeeting(Long id) {
        return updateMeetingStatus(id, "ACTIVE");
    }

    // Завершить встречу
    @Transactional
    public Meeting completeMeeting(Long id) {
        return updateMeetingStatus(id, "COMPLETED");
    }

    // Отменить встречу
    @Transactional
    public Meeting cancelMeeting(Long id) {
        return updateMeetingStatus(id, "CANCELLED");
    }

    // Обновление информации о встрече
    @Transactional
    public Meeting updateMeeting(Long id, UpdateMeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        if (request.getTopic() != null) {
            meeting.setTopic(request.getTopic());
        }
        if (request.getScheduledTime() != null) {
            meeting.setScheduledTime(request.getScheduledTime());
        }
        if (request.getDurationMinutes() != null) {
            meeting.setDurationMinutes(request.getDurationMinutes());
        }

        return meetingRepository.save(meeting);
    }

    // Удаление встречи
    @Transactional
    public void deleteMeeting(Long id) {
        if (meetingRepository.existsById(id)) {
            meetingRepository.deleteById(id);
            log.info("Meeting deleted with ID: {}", id);
        } else {
            throw new RuntimeException("Meeting not found with id: " + id);
        }
    }

    // Отправить приглашение повторно
    @Transactional
    public void resendInvitation(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        if (meeting.getPatientEmail() != null && !meeting.getPatientEmail().trim().isEmpty()) {
            sendInvitation(meeting);
        } else {
            throw new RuntimeException("Patient email not found for meeting id: " + id);
        }
    }

    // DTO для создания встречи
    @Data
    public static class CreateMeetingRequest {
        private Integer appointmentId;
        private Integer doctorId;
        private Integer patientId;
        private String topic;
        private String description;
        private LocalDateTime scheduledTime;
        private Integer durationMinutes;
        private String patientEmail;
    }

    // DTO для обновления встречи
    @Data
    public static class UpdateMeetingRequest {
        private String topic;
        private String description;
        private LocalDateTime scheduledTime;
        private Integer durationMinutes;
    }
}
