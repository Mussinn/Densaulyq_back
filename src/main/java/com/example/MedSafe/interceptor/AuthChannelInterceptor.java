package com.example.MedSafe.interceptor;

import com.example.MedSafe.service.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public AuthChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Обрабатываем только CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    // Извлекаем username (userId) из токена
                    String userId = jwtUtil.extractUsername(token);

                    // Устанавливаем Principal — это ключевой момент!
                    accessor.setUser(new UsernamePrincipal(userId));

                    // Дополнительно сохраняем в атрибуты сессии (на всякий случай)
                    accessor.getSessionAttributes().put("userId", userId);

                    log.info("STOMP CONNECT: authenticated user {}", userId);
                } catch (Exception e) {
                    log.warn("Invalid JWT in STOMP CONNECT: {}", e.getMessage());
                }
            } else {
                log.warn("No Authorization header in STOMP CONNECT");
            }
        }

        return message;
    }
}