package com.example.MedSafe.controller;

import com.example.MedSafe.model.Meeting;
import com.example.MedSafe.model.MeetingConsilium;
import com.example.MedSafe.service.MeetingConsiliumService;
import com.example.MedSafe.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meeting-consilium")
@Slf4j
public class MeetingConsiliumController {
    private final MeetingConsiliumService meetingConsiliumService;
    @Operation(summary = "Создать новую встречу")
    @PostMapping
    public ResponseEntity<MeetingConsilium> createMeeting(@RequestBody MeetingConsiliumService.CreateMeetingRequest request) {
        log.info("createMeeting called");
        MeetingConsilium meeting = meetingConsiliumService.createMeeting(request);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Получить все встречи доктора")
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MeetingConsilium>> getDoctorMeetings(@PathVariable Integer doctorId) {
        List<MeetingConsilium> meetings = meetingConsiliumService.getDoctorMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить все встречи доктора")
    @GetMapping("/receiver-doctor/{receiverDoctorId}")
    public ResponseEntity<List<MeetingConsilium>> getReceiverDoctorMeetings(@PathVariable Integer receiverDoctorId) {
        List<MeetingConsilium> meetings = meetingConsiliumService.getReceiverDoctorMeetings(receiverDoctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить активные встречи доктора")
    @GetMapping("/doctor/{doctorId}/active")
    public ResponseEntity<List<MeetingConsilium>> getActiveMeetings(@PathVariable Integer doctorId) {
        List<MeetingConsilium> meetings = meetingConsiliumService.getActiveMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить предстоящие встречи")
    @GetMapping("/doctor/{doctorId}/upcoming")
    public ResponseEntity<List<MeetingConsilium>> getUpcomingMeetings(@PathVariable Integer doctorId) {
        List<MeetingConsilium> meetings = meetingConsiliumService.getUpcomingMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить встречу по ID")
    @GetMapping("/{id}")
    public ResponseEntity<MeetingConsilium> getMeetingById(@PathVariable Integer id) {
        return meetingConsiliumService.getMeetingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить встречу по roomId")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<MeetingConsilium> getMeetingByRoomId(@PathVariable String roomId) {
        return meetingConsiliumService.getMeetingByRoomId(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Обновить статус встречи")
    @PatchMapping("/{id}/status")
    public ResponseEntity<MeetingConsilium> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        MeetingConsilium meeting = meetingConsiliumService.updateMeetingStatus(id, status);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Начать встречу")
    @PostMapping("/{id}/start")
    public ResponseEntity<MeetingConsilium> startMeeting(@PathVariable Integer id) {
        MeetingConsilium meeting = meetingConsiliumService.startMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Завершить встречу")
    @PostMapping("/{id}/complete")
    public ResponseEntity<MeetingConsilium> completeMeeting(@PathVariable Integer id) {
        MeetingConsilium meeting = meetingConsiliumService.completeMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Отменить встречу")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<MeetingConsilium> cancelMeeting(@PathVariable Integer id) {
        MeetingConsilium meeting = meetingConsiliumService.cancelMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Обновить информацию о встрече")
    @PutMapping("/{id}")
    public ResponseEntity<MeetingConsilium> updateMeeting(
            @PathVariable Integer id,
            @RequestBody MeetingService.UpdateMeetingRequest request) {
        MeetingConsilium meeting = meetingConsiliumService.updateMeeting(id, request);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Удалить встречу")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Integer id) {
        meetingConsiliumService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

//    @Operation(summary = "Отправить приглашение повторно")
//    @PostMapping("/{id}/resend-invite")
//    public ResponseEntity<Void> resendInvitation(@PathVariable Long id) {
//        meetingConsiliumService.resendInvitation(id);
//        return ResponseEntity.ok().build();
//    }

    @Operation(summary = "Быстрое создание встречи (простой вариант)")
    @GetMapping("/create-simple")
    public ResponseEntity<Map<String, String>> createSimpleMeeting(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(required = false) Integer patientId,
            @RequestParam(required = false) String patientEmail) {

        String roomId = generateRoomId(topic);
        String meetingUrl = "https://meet.jit.si/" + roomId;

        // Сохраняем в базу, если передан doctorId
        if (doctorId != null) {
            MeetingConsiliumService.CreateMeetingRequest request = new MeetingConsiliumService.CreateMeetingRequest();
            request.setSenderDoctorId(doctorId);
            request.setReceiverDoctorId(patientId);
            request.setTopic(topic != null ? topic : "Консультация");
            request.setReceiverDoctorEmail(patientEmail);
            request.setScheduledTime(LocalDateTime.now().plusHours(1));

            meetingConsiliumService.createMeeting(request);
        }

        return ResponseEntity.ok(Map.of(
                "meetingUrl", meetingUrl,
                "roomId", roomId,
                "message", "Ссылка действительна 24 часа"
        ));
    }

    @GetMapping("/share")
    public void shareLink(
            @RequestParam String email,
            @RequestParam String link) {
        try {
            System.out.println("Получен запрос на отправку:");
            System.out.println("Email: " + email);
            System.out.println("Link: " + link);

            meetingConsiliumService.shareLink(link, email);

            System.out.println("Письмо успешно отправлено");
        } catch (Exception e) {
            System.err.println("Ошибка при отправке: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String generateRoomId(String topic) {
        String baseId = UUID.randomUUID().toString().substring(0, 8);
        if (topic != null && !topic.trim().isEmpty()) {
            return topic.toLowerCase()
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-") + "-" + baseId;
        }
        return "meeting-" + baseId;
    }
}
