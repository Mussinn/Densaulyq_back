package com.example.MedSafe.service;

import com.example.MedSafe.model.CallHistory;
import com.example.MedSafe.model.dto.CallRequest;
import com.example.MedSafe.model.dto.CallResponse;
import com.example.MedSafe.model.dto.CallNotification;
import com.example.MedSafe.model.dto.WebRTCMessage;
import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.CallHistoryRepository;
import com.example.MedSafe.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@Slf4j
public class CallService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CallHistoryRepository callHistoryRepository;

    // Храним активные соединения пользователей: userId -> sessionId
    private final Map<String, String> activeUsers = new ConcurrentHashMap<>();

    // Храним активные звонки: callId -> информация о звонке
    private final Map<String, CallRequest> activeCalls = new ConcurrentHashMap<>();

    // Храним комнаты WebRTC: callId -> roomId
    private final Map<String, String> callRooms = new ConcurrentHashMap<>();

    /**
     * Регистрация пользователя в системе звонков
     */
    public void registerUser(String userId, String sessionId) {
        activeUsers.put(userId, sessionId);
        log.info("User {} registered with session {}", userId, sessionId);

        // Обновляем статус пользователя в БД
        userRepository.findById(Integer.valueOf(userId)).ifPresent(user -> {
            user.setOnline(true);
            userRepository.save(user);
        });
    }

    /**
     * Отключение пользователя
     */
    public void unregisterUser(String userId) {
        activeUsers.remove(userId);
        log.info("User {} disconnected", userId);

        // Обновляем статус в БД
        userRepository.findById(Integer.valueOf(userId)).ifPresent(user -> {
            user.setOnline(false);
            userRepository.save(user);
        });
    }

    /**
     * Проверка онлайн статуса пользователя
     */
    public boolean isUserOnline(String userId) {
        return activeUsers.containsKey(userId);
    }

    /**
     * Проверка доступности доктора
     */
    public boolean isDoctorAvailable(Long doctorId) {
        String doctorIdStr = doctorId.toString();

        // Проверяем в активных пользователях
        if (!activeUsers.containsKey(doctorIdStr)) {
            return false;
        }

        // Проверяем, не занят ли доктор другим звонком
        return activeCalls.values().stream()
                .noneMatch(call -> call.getTargetId().equals(doctorIdStr) &&
                        call.getTimestamp() > System.currentTimeMillis() - 300000); // 5 минут
    }

    /**
     * Инициирование звонка
     */
    public CallNotification initiateCall(CallRequest request) {
        // Генерируем ID звонка если не предоставлен
        if (request.getCallId() == null || request.getCallId().isEmpty()) {
            request.setCallId(UUID.randomUUID().toString());
        }

        // Сохраняем информацию о звонке
        activeCalls.put(request.getCallId(), request);

        // Создаем уведомление для получателя
        return CallNotification.builder()
                .callId(request.getCallId())
                .callerId(request.getCallerId())
                .callerName(request.getCallerName())
                .callerType(request.getCallerType())
                .type("incoming")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Принятие звонка
     */
    public CallResponse acceptCall(String callId, String doctorId, String doctorName) {
        CallRequest callRequest = activeCalls.get(callId);
        if (callRequest == null) {
            throw new IllegalArgumentException("Call not found: " + callId);
        }

        // Создаем комнату для WebRTC
        String roomId = "medsafe-call-" + callId;
        callRooms.put(callId, roomId);

        // Создаем ответ
        return CallResponse.builder()
                .callId(callId)
                .status("accepted")
                .doctorId(doctorId)
                .doctorName(doctorName)
                .patientId(callRequest.getCallerId())
                .patientName(callRequest.getCallerName())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Отклонение звонка
     */
    public CallResponse rejectCall(String callId, String reason) {
        CallRequest callRequest = activeCalls.remove(callId);
        if (callRequest == null) {
            throw new IllegalArgumentException("Call not found: " + callId);
        }

        // Сохраняем в историю
        saveCallToHistory(callRequest, "REJECTED", reason);

        return CallResponse.builder()
                .callId(callId)
                .status("rejected")
                .reason(reason)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Завершение звонка
     */
    public CallResponse endCall(String callId, String endedById) {
        CallRequest callRequest = activeCalls.remove(callId);
        callRooms.remove(callId);

        if (callRequest == null) {
            // Может быть уже завершен
            return CallResponse.builder()
                    .callId(callId)
                    .status("ended")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        // Определяем кто завершил
        String status = callRequest.getCallerId().equals(endedById) ?
                "ENDED_BY_CALLER" : "ENDED_BY_RECEIVER";

        // Сохраняем в историю
        saveCallToHistory(callRequest, status, "Call ended normally");

        return CallResponse.builder()
                .callId(callId)
                .status("ended")
                .patientId(callRequest.getCallerId())
                .patientName(callRequest.getCallerName())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Получение информации о комнате WebRTC
     */
    public String getCallRoom(String callId) {
        return callRooms.get(callId);
    }

    /**
     * Получение активного звонка пользователя
     */
    public CallRequest getActiveCallForUser(String userId) {
        return activeCalls.values().stream()
                .filter(call -> call.getCallerId().equals(userId) || call.getTargetId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Сохранение звонка в историю
     */
    private void saveCallToHistory(CallRequest callRequest, String status, String notes) {
        try {
            CallHistory callHistory = new CallHistory();
            callHistory.setCallId(callRequest.getCallId());
            callHistory.setCallerId(Long.parseLong(callRequest.getCallerId()));
            callHistory.setReceiverId(Long.parseLong(callRequest.getTargetId()));
            callHistory.setCallType(callRequest.getCallerType());
            callHistory.setStatus(status);
            callHistory.setDuration(0L); // Можно рассчитать по timestamp
            callHistory.setNotes(notes);
            callHistory.setCreatedAt(LocalDateTime.now());

            callHistoryRepository.save(callHistory);
            log.info("Call saved to history: {}", callRequest.getCallId());

        } catch (Exception e) {
            log.error("Error saving call to history: {}", e.getMessage());
        }
    }

    /**
     * Обработка пропущенного звонка
     */
    public CallNotification markCallAsMissed(String callId) {
        CallRequest callRequest = activeCalls.remove(callId);
        if (callRequest == null) return null;

        // Сохраняем в историю как пропущенный
        saveCallToHistory(callRequest, "MISSED", "Call was missed");

        return CallNotification.builder()
                .callId(callId)
                .callerId(callRequest.getCallerId())
                .callerName(callRequest.getCallerName())
                .callerType(callRequest.getCallerType())
                .type("missed")
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
