//package com.example.MedSafe.controller;
//
//import com.example.MedSafe.dto.*;
//import com.example.MedSafe.model.dto.*;
//import com.example.MedSafe.service.CallService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@RestController
//@RequestMapping("/api/audio-call")
//@RequiredArgsConstructor
//public class AudioCallController {
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @Autowired
//    private CallService callService;
//
//    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
//
//    // Подключение пользователя к WebSocket
//    @MessageMapping("/call.connect")
//    public void connect(@Payload CallConnectRequest request) {
//        String userId = request.getUserId();
//        String sessionId = request.getSessionId();
//        userSessions.put(userId, sessionId);
//        System.out.println("User connected: " + userId + ", session: " + sessionId);
//    }
//
//    // Инициация звонка от пациента к доктору
//    @MessageMapping("/call.initiate")
//    public void initiateCall(@Payload CallRequest request) {
//        System.out.println("Call initiated from " + request.getCallerId() + " to " + request.getTargetId());
//
//        // Отправляем уведомление доктору
//        CallNotification notification = CallNotification.builder()
//                .callId(request.getCallId())
//                .callerId(request.getCallerId())
//                .callerName(request.getCallerName())
//                .callerType("patient")
//                .timestamp(System.currentTimeMillis())
//                .type("incoming")
//                .build();
//
//        // Отправляем конкретному доктору
//        messagingTemplate.convertAndSendToUser(
//                request.getTargetId(),
//                "/queue/call",
//                notification
//        );
//
//        // Сохраняем информацию о звонке
//        callService.initiateCall(request);
//    }
//
//    // Доктор принимает звонок
//    @MessageMapping("/call.accept")
//    public void acceptCall(@Payload CallAcceptRequest request) {
//        System.out.println("Call accepted: " + request.getCallId());
//
//        // Отправляем пациенту, что доктор принял звонок
//        CallResponse response = CallResponse.builder()
//                .callId(request.getCallId())
//                .status("accepted")
//                .doctorId(request.getDoctorId())
//                .doctorName(request.getDoctorName())
//                .timestamp(System.currentTimeMillis())
//                .build();
//
//        messagingTemplate.convertAndSendToUser(
//                request.getPatientId(),
//                "/queue/call-response",
//                response
//        );
//
//        // Создаем комнату для WebRTC
//        String roomId = "call-room-" + request.getCallId();
//        callService.createCallRoom(request.getCallId(), roomId);
//    }
//
//    // Отклонение звонка
//    @MessageMapping("/call.reject")
//    public void rejectCall(@Payload CallRejectRequest request) {
//        CallResponse response = CallResponse.builder()
//                .callId(request.getCallId())
//                .status("rejected")
//                .reason(request.getReason())
//                .timestamp(System.currentTimeMillis())
//                .build();
//
//        messagingTemplate.convertAndSendToUser(
//                request.getPatientId(),
//                "/queue/call-response",
//                response
//        );
//    }
//
//    // Завершение звонка
//    @MessageMapping("/call.end")
//    public void endCall(@Payload CallEndRequest request) {
//        CallResponse response = CallResponse.builder()
//                .callId(request.getCallId())
//                .status("ended")
//                .timestamp(System.currentTimeMillis())
//                .build();
//
//        // Отправляем обоим участникам
//        messagingTemplate.convertAndSendToUser(
//                request.getPatientId(),
//                "/queue/call-response",
//                response
//        );
//
//        messagingTemplate.convertAndSendToUser(
//                request.getDoctorId(),
//                "/queue/call-response",
//                response
//        );
//
//        // Обновляем статус звонка
//        callService.endCall(request.getCallId());
//    }
//
//    // WebRTC signaling - обмен SDP и ICE кандидатами
//    @MessageMapping("/call.signal")
//    public void handleSignal(@Payload SignalMessage message) {
//        // Пересылаем сигнал другому участнику
//        messagingTemplate.convertAndSendToUser(
//                message.getTargetUserId(),
//                "/queue/signal",
//                message
//        );
//    }
//
//    // Проверка доступности доктора
//    @GetMapping("/api/v1/doctor/{doctorId}/availability")
//    public Map<String, Boolean> checkAvailability(@PathVariable Long doctorId) {
//        boolean isAvailable = callService.isDoctorAvailable(doctorId);
//        return Map.of("available", isAvailable);
//    }
//}
