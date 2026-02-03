package com.example.MedSafe.controller;

import com.example.MedSafe.model.dto.*;
import com.example.MedSafe.service.CallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
public class CallController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CallService callService;

    /**
     * Подключение пользователя к системе звонков
     */
    @MessageMapping("/call.connect")
    public void handleConnect(@Payload Map<String, String> connectRequest) {
        log.info("Получен CONNECT запрос: {}", connectRequest);
        log.info("Headers из запроса: userId={}, sessionId={}, role={}, name={}",
                connectRequest.get("userId"),
                connectRequest.get("sessionId"),
                connectRequest.get("userRole"),
                connectRequest.get("userName"));
        String userId = connectRequest.get("userId");
        String sessionId = connectRequest.get("sessionId");

        callService.registerUser(userId, sessionId);
        log.info("User connected - ID: {}, Session: {}", userId, sessionId);
    }

    /**
     * Отключение пользователя
     */
    @MessageMapping("/call.disconnect")
    public void handleDisconnect(@Payload Map<String, String> disconnectRequest) {
        String userId = disconnectRequest.get("userId");

        callService.unregisterUser(userId);
        log.info("User disconnected - ID: {}", userId);

        // Проверяем активные звонки пользователя
        CallRequest activeCall = callService.getActiveCallForUser(userId);
        if (activeCall != null) {
            // Помечаем звонок как пропущенный
            CallNotification missedCall = callService.markCallAsMissed(activeCall.getCallId());

            // Уведомляем другую сторону
            String targetId = activeCall.getCallerId().equals(userId) ?
                    activeCall.getTargetId() : activeCall.getCallerId();

            messagingTemplate.convertAndSendToUser(
                    targetId,
                    "/queue/call",
                    missedCall
            );
        }
    }

    /**
     * Инициирование звонка
     */
    @MessageMapping("/call.initiate")
    public void handleInitiateCall(@Payload CallRequest callRequest) {
        log.info("InitiateCall получен от {} к {}",
                callRequest.getCallerId(), callRequest.getTargetId());

        CallNotification notification = callService.initiateCall(callRequest);

        String targetUserId = callRequest.getTargetId(); // это строка "2"

        log.info("Отправляем уведомление доктору {}: callId={}, caller={}",
                targetUserId, notification.getCallId(), callRequest.getCallerName());

        // ПРАВИЛЬНЫЙ СПОСОБ — только так!
        messagingTemplate.convertAndSendToUser(
                targetUserId,                // "2" — строка!
                "/queue/call",               // без /user/
                notification
        );

        log.info("Уведомление отправлено в персональную очередь: /user/{}/queue/call", targetUserId);
    }

    /**
     * Принятие звонка
     */
    @MessageMapping("/call.accept")
    @SendToUser("/queue/call-response")
    public CallResponse handleAcceptCall(@Payload Map<String, String> acceptRequest) {
        String callId = acceptRequest.get("callId");
        String doctorId = acceptRequest.get("doctorId");
        String doctorName = acceptRequest.get("doctorName");

        log.info("Call {} accepted by doctor {}", callId, doctorName);

        CallResponse response = callService.acceptCall(callId, doctorId, doctorName);

        // Отправляем также пациенту
        messagingTemplate.convertAndSendToUser(
                response.getPatientId(),
                "/queue/call-response",
                response
        );

        return response;
    }

    /**
     * Отклонение звонка
     */
    @MessageMapping("/call.reject")
    @SendToUser("/queue/call-response")
    public CallResponse handleRejectCall(@Payload Map<String, String> rejectRequest) {
        String callId = rejectRequest.get("callId");
        String reason = rejectRequest.get("reason") != null ?
                rejectRequest.get("reason") : "Звонок отклонен";

        log.info("Call {} rejected. Reason: {}", callId, reason);

        CallResponse response = callService.rejectCall(callId, reason);

        // Отправляем также звонящему
        CallRequest callRequest = callService.getActiveCallForUser(response.getPatientId());
        if (callRequest != null) {
            messagingTemplate.convertAndSendToUser(
                    callRequest.getCallerId(),
                    "/queue/call-response",
                    response
            );
        }

        return response;
    }

    /**
     * Завершение звонка
     */
    @MessageMapping("/call.end")
    @SendToUser("/queue/call-response")
    public CallResponse handleEndCall(@Payload Map<String, String> endRequest) {
        String callId = endRequest.get("callId");
        String endedById = endRequest.get("endedById");

        log.info("Call {} ended by user {}", callId, endedById);

        CallResponse response = callService.endCall(callId, endedById);

        // Отправляем другому участнику
        CallRequest callRequest = callService.getActiveCallForUser(endedById);
        if (callRequest != null) {
            String otherUserId = callRequest.getCallerId().equals(endedById) ?
                    callRequest.getTargetId() : callRequest.getCallerId();

            messagingTemplate.convertAndSendToUser(
                    otherUserId,
                    "/queue/call-response",
                    response
            );
        }

        return response;
    }

    /**
     * Обмен WebRTC сигналами
     */
    @MessageMapping("/call.webrtc")
    public void handleWebRTCSignal(@Payload WebRTCMessage message) {
        log.debug("WebRTC signal from {} to {}: {}",
                message.getSenderId(), message.getTargetId(), message.getType());

        // Пересылаем сигнал получателю
        messagingTemplate.convertAndSendToUser(
                message.getTargetId(),
                "/queue/webrtc",
                message
        );
    }

    /**
     * Проверка доступности доктора (REST endpoint)
     */
    @GetMapping("/api/v1/call/doctor/{doctorId}/available")
    public Map<String, Object> checkDoctorAvailability(@PathVariable Long doctorId) {
        boolean isAvailable = callService.isDoctorAvailable(doctorId);
        boolean isOnline = callService.isUserOnline(doctorId.toString());

        return Map.of(
                "available", isAvailable,
                "online", isOnline,
                "doctorId", doctorId,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Получение активного звонка пользователя
     */
    @GetMapping("/api/v1/call/user/{userId}/active")
    public Map<String, Object> getActiveCall(@PathVariable Long userId) {
        CallRequest activeCall = callService.getActiveCallForUser(userId.toString());

        if (activeCall != null) {
            return Map.of(
                    "hasActiveCall", true,
                    "callId", activeCall.getCallId(),
                    "callerId", activeCall.getCallerId(),
                    "callerName", activeCall.getCallerName(),
                    "targetId", activeCall.getTargetId(),
                    "timestamp", activeCall.getTimestamp()
            );
        }

        return Map.of("hasActiveCall", false);
    }

    /**
     * Ping для поддержания соединения
     */
    @MessageMapping("/call.ping")
    @SendToUser("/queue/pong")
    public Map<String, Object> handlePing(@Payload Map<String, String> ping) {
        String userId = ping.get("userId");
        long clientTime = Long.parseLong(ping.get("timestamp"));
        long serverTime = System.currentTimeMillis();
        long delay = serverTime - clientTime;

        log.info("Получен ping от user {} | клиентское время: {} | задержка: {} мс", userId, clientTime, delay);

        Map<String, Object> response = Map.of(
                "type", "pong",
                "timestamp", serverTime,
                "userId", userId
        );

        log.debug("Отправлен pong пользователю {}", userId);
        return response;
    }
}