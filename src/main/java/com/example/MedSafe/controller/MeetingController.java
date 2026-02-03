package com.example.MedSafe.controller;

import com.example.MedSafe.model.Meeting;
import com.example.MedSafe.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Video Meetings", description = "API для управления видеовстречами")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "Создать новую встречу")
    @PostMapping
    public ResponseEntity<Meeting> createMeeting(@RequestBody MeetingService.CreateMeetingRequest request) {
        Meeting meeting = meetingService.createMeeting(request);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Получить все встречи доктора")
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Meeting>> getDoctorMeetings(@PathVariable Long doctorId) {
        List<Meeting> meetings = meetingService.getDoctorMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить активные встречи доктора")
    @GetMapping("/doctor/{doctorId}/active")
    public ResponseEntity<List<Meeting>> getActiveMeetings(@PathVariable Long doctorId) {
        List<Meeting> meetings = meetingService.getActiveMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить предстоящие встречи")
    @GetMapping("/doctor/{doctorId}/upcoming")
    public ResponseEntity<List<Meeting>> getUpcomingMeetings(@PathVariable Long doctorId) {
        List<Meeting> meetings = meetingService.getUpcomingMeetings(doctorId);
        return ResponseEntity.ok(meetings);
    }

    @Operation(summary = "Получить встречу по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Meeting> getMeetingById(@PathVariable Long id) {
        return meetingService.getMeetingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Получить встречу по roomId")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Meeting> getMeetingByRoomId(@PathVariable String roomId) {
        return meetingService.getMeetingByRoomId(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Обновить статус встречи")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Meeting> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        Meeting meeting = meetingService.updateMeetingStatus(id, status);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Начать встречу")
    @PostMapping("/{id}/start")
    public ResponseEntity<Meeting> startMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.startMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Завершить встречу")
    @PostMapping("/{id}/complete")
    public ResponseEntity<Meeting> completeMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.completeMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Отменить встречу")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Meeting> cancelMeeting(@PathVariable Long id) {
        Meeting meeting = meetingService.cancelMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Обновить информацию о встрече")
    @PutMapping("/{id}")
    public ResponseEntity<Meeting> updateMeeting(
            @PathVariable Long id,
            @RequestBody MeetingService.UpdateMeetingRequest request) {
        Meeting meeting = meetingService.updateMeeting(id, request);
        return ResponseEntity.ok(meeting);
    }

    @Operation(summary = "Удалить встречу")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Отправить приглашение повторно")
    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<Void> resendInvitation(@PathVariable Long id) {
        meetingService.resendInvitation(id);
        return ResponseEntity.ok().build();
    }

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
            MeetingService.CreateMeetingRequest request = new MeetingService.CreateMeetingRequest();
            request.setDoctorId(doctorId);
            request.setPatientId(patientId);
            request.setTopic(topic != null ? topic : "Консультация");
            request.setPatientEmail(patientEmail);
            request.setScheduledTime(LocalDateTime.now().plusHours(1));

            meetingService.createMeeting(request);
        }

        return ResponseEntity.ok(Map.of(
                "meetingUrl", meetingUrl,
                "roomId", roomId,
                "message", "Ссылка действительна 24 часа"
        ));
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