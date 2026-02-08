package com.example.MedSafe.service;

import com.example.MedSafe.model.MeetingConsilium;
import com.example.MedSafe.repository.MeetingConsiliumRepository;
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
@Slf4j
@RequiredArgsConstructor
public class MeetingConsiliumService {

    private final MeetingConsiliumRepository meetingConsiliumRepository;
    private final MailService emailService;

    // Создание новой встречи
    @Transactional
    public MeetingConsilium createMeeting(MeetingConsiliumService.CreateMeetingRequest request) {
        String roomId = generateRoomId(request.getTopic());
        String meetingUrl = "https://meet.jit.si/" + roomId;

        MeetingConsilium meeting = new MeetingConsilium();
        meeting.setRoomId(roomId);
        meeting.setSenderDoctorId(request.getSenderDoctorId());
        meeting.setReceiverDoctorId(request.getReceiverDoctorId());
        meeting.setRoomId(roomId);
        meeting.setMeetingUrl(meetingUrl);
        meeting.setTopic(request.getTopic());
        meeting.setStatus("SCHEDULED");
        meeting.setScheduledTime(request.getScheduledTime() != null ? request.getScheduledTime() : LocalDateTime.now().plusHours(1));
        meeting.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30);


        MeetingConsilium savedMeeting = meetingConsiliumRepository.save(meeting);

        // Отправка приглашения пациенту
        sendInvitation(savedMeeting, request.getReceiverDoctorEmail());

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
    private void sendInvitation(MeetingConsilium meeting, String receiverDoctorEmail) {
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
            sendLink(receiverDoctorEmail, subject, body);
            meetingConsiliumRepository.save(meeting);
            log.info("Invitation sent for meeting ID: {}", meeting.getId());
        } catch (Exception e) {
            log.error("Failed to send invitation for meeting ID: {}", meeting.getId(), e);
        }
    }

    private void sendLink(String email,String subject,String body) {
        try {
            emailService.send(email, subject, body);
        } catch (Exception e) {
            log.error("Failed to send invitation for meeting", e);
        }
    }

    // Получение всех встреч доктора receiverDoctorId senderDoctorId
    public List<MeetingConsilium> getDoctorMeetings(Integer senderDoctorId) {
        return meetingConsiliumRepository.findBySenderDoctorId(senderDoctorId);
    }

    public List<MeetingConsilium> getReceiverDoctorMeetings(Integer senderDoctorId) {
        return meetingConsiliumRepository.findByReceiverDoctorId(senderDoctorId);
    }

    // Получение активных встреч доктора
    public List<MeetingConsilium> getActiveMeetings(Integer doctorId) {
        return meetingConsiliumRepository.findActiveBySenderDoctorId(doctorId);
    }

    // Получение предстоящих встреч
    public List<MeetingConsilium> getUpcomingMeetings(Integer doctorId) {
        return meetingConsiliumRepository.findBySenderDoctorIdAndScheduledTimeAfter(doctorId, LocalDateTime.now());
    }

    // Поиск встречи по ID
    public Optional<MeetingConsilium> getMeetingById(Integer id) {
        return meetingConsiliumRepository.findById(id);
    }

    // Поиск встречи по roomId
    public Optional<MeetingConsilium> getMeetingByRoomId(String roomId) {
        return meetingConsiliumRepository.findByRoomId(roomId);
    }

    // Обновление статуса встречи
    @Transactional
    public MeetingConsilium updateMeetingStatus(Integer id, String status) {
        MeetingConsilium meeting = meetingConsiliumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));

        String oldStatus = meeting.getStatus();
        meeting.setStatus(status);

        MeetingConsilium updated = meetingConsiliumRepository.save(meeting);
        log.info("Meeting ID: {} status changed from {} to {}", id, oldStatus, status);

        return updated;
    }

    // Начать встречу
    @Transactional
    public MeetingConsilium startMeeting(Integer id) {
        return updateMeetingStatus(id, "ACTIVE");
    }

    // Завершить встречу
    @Transactional
    public MeetingConsilium completeMeeting(Integer id) {
        return updateMeetingStatus(id, "COMPLETED");
    }

    // Отменить встречу
    @Transactional
    public MeetingConsilium cancelMeeting(Integer id) {
        return updateMeetingStatus(id, "CANCELLED");
    }

    // Обновление информации о встрече
    @Transactional
    public MeetingConsilium updateMeeting(Integer id, MeetingService.UpdateMeetingRequest request) {
        MeetingConsilium meeting = meetingConsiliumRepository.findById(id)
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

        return meetingConsiliumRepository.save(meeting);
    }

    // Удаление встречи
    @Transactional
    public void deleteMeeting(Integer id) {
        if (meetingConsiliumRepository.existsById(id)) {
            meetingConsiliumRepository.deleteById(id);
            log.info("Meeting deleted with ID: {}", id);
        } else {
            throw new RuntimeException("Meeting not found with id: " + id);
        }
    }

//    // Отправить приглашение повторно
//    @Transactional
//    public void resendInvitation(Long id) {
//        MeetingConsilium meeting = meetingConsiliumRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));
//
//        sendInvitation(meeting);
//    }

    public void shareLink(String meetingUrl, String email) {
        try {
            String subject = "Приглашение на видеоконсультацию";
            String body = String.format(
                    """
                    Уважаемый коллега,
                    
                    С вами поделились ссылкой на видеоконсультацию.
                    
                    Ссылка для подключения: %s
                    
                    С уважением,
                    Медицинская система MedSafe
                    """,
                    meetingUrl
            );
            sendLink(email, body, subject);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send invitation for meeting", e);
        }
    }

    // DTO для создания встречи
    @Data
    public static class CreateMeetingRequest {
        private Integer appointmentId;
        private Integer senderDoctorId;
        private Integer receiverDoctorId;
        private String topic;
        private String description;
        private LocalDateTime scheduledTime;
        private Integer durationMinutes;
            private String receiverDoctorEmail;
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
