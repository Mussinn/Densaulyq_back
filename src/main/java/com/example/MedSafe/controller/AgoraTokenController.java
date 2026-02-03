package com.example.MedSafe.controller;

import com.example.MedSafe.util.RtcTokenBuilder2;
import com.example.MedSafe.util.RtcTokenBuilder2.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agora")
public class AgoraTokenController {

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @GetMapping("/token")
    public ResponseEntity<?> generateToken(
            @RequestParam String channelName,
            @RequestParam(defaultValue = "0") int uid,          // 0 = Agora сам назначит, или передавай свой user_id
            @RequestParam(defaultValue = "7200") int expireSeconds  // 2 часа по умолчанию
    ) {
        try {
            // Создаём экземпляр билдера
            RtcTokenBuilder2 builder = new RtcTokenBuilder2();

            // Самый простой вариант: publisher + одинаковый срок для всех привилегий
            // (подходит для большинства аудио/видео звонков 1:1 или group)
            String token = builder.buildTokenWithUid(
                    appId,
                    appCertificate,
                    channelName,
                    uid,
                    Role.ROLE_PUBLISHER,          // ← Publisher, чтобы можно было говорить (микрофон)
                    expireSeconds,                // tokenExpire
                    expireSeconds                 // privilegeExpire (для join + publish)
            );

            // Если хочешь роль attendee/subscriber (только слушать) — используй ROLE_SUBSCRIBER
            // String token = builder.buildTokenWithUid(appId, appCertificate, channelName, uid, Role.ROLE_SUBSCRIBER, expireSeconds, expireSeconds);

            // Более гибкий вариант (отдельные сроки на join / publish audio / publish video)
            // String token = builder.buildTokenWithUid(
            //     appId, appCertificate, channelName, uid,
            //     expireSeconds,          // tokenExpire
            //     expireSeconds,          // joinChannelPrivilegeExpire
            //     expireSeconds,          // pubAudioPrivilegeExpire
            //     expireSeconds,          // pubVideoPrivilegeExpire (0 если только аудио)
            //     0                       // pubDataStreamPrivilegeExpire (обычно 0)
            // );

            TokenResponse response = new TokenResponse(token, uid, channelName, appId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();  // для логов
            return ResponseEntity.badRequest().body("Ошибка генерации токена: " + e.getMessage());
        }
    }

    // Можно оставить как static class или вынести в отдельный файл
    public static class TokenResponse {
        public String token;
        public int uid;
        public String channel;
        public String appId;

        public TokenResponse(String token, int uid, String channel, String appId) {
            this.token = token;
            this.uid = uid;
            this.channel = channel;
            this.appId = appId;
        }
    }
}