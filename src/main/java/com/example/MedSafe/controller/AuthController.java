package com.example.MedSafe.controller;

import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.AuthResponse;
import com.example.MedSafe.service.AuditService;
import com.example.MedSafe.service.AuthService;
import com.example.MedSafe.service.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuditService auditService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            logger.info("Получен запрос на регистрацию: username={}, roleId={}",
                    request.getUsername(), request.getRoleId());

            // Валидация обязательных полей
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Username is required");
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Password is required");
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new RuntimeException("Email is required");
            }
            if (request.getRoleId() == null) {
                throw new RuntimeException("RoleId is required");
            }

            // Валидация специфичных полей для роли
            if (request.getRoleId() == 2) {
                // Доктор - требуется specialization
                if (request.getSpecialization() == null || request.getSpecialization().trim().isEmpty()) {
                    throw new RuntimeException("Specialization is required for doctors");
                }
            } else if (request.getRoleId() == 3) {
                // Пациент - требуется gender
                if (request.getGender() == null || request.getGender().trim().isEmpty()) {
                    throw new RuntimeException("Gender is required for patients");
                }
            }

            User user = authService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getRoleId(),
                    request.getSpecialization(),    // Для доктора
                    request.getContactNumber(),     // Для обоих
                    request.getAddress(),           // Для пациента
                    request.getGender()             // Для пациента
            );

            auditService.log(
                    user, "REGISTER", "users", user.getUserId(), httpRequest.getRemoteAddr());

            logger.info("Регистрация успешна: userId={}", user.getUserId());
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            logger.error("Ошибка при регистрации: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Ошибка при регистрации пользователя");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.loginUser(request.getUsername(), request.getPassword());

            auditService.log(
                    user, "LOGIN", "users", user.getUserId(), httpRequest.getRemoteAddr());

            String token = jwtUtil.generateToken(user.getUsername());
            AuthResponse authResponse = new AuthResponse(user.getUserId(), user.getUsername(), token, user.getRoles());

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            logger.error("Ошибка при входе: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/login/key")
    public ResponseEntity<?> loginWithPrivateKey(@RequestBody LoginKeyRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.loginWithPrivateKey(request.getUsername(), request.getPrivateKey());

            auditService.log(
                    user, "LOGIN_WITH_KEY", "users", user.getUserId(), httpRequest.getRemoteAddr());

            String token = jwtUtil.generateToken(user.getUsername());
            AuthResponse authResponse = new AuthResponse(user.getUserId(), user.getUsername(), token, user.getRoles());

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            logger.error("Ошибка при входе по ключу: {}", e.getMessage());
            auditService.log(
                    null, "FAILED_LOGIN_WITH_KEY", "users", null, httpRequest.getRemoteAddr());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Не удалось аутентифицироваться с помощью приватного ключа: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @Getter
    @Setter
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String firstName;
        private String lastName;
        private Integer roleId;

        // Поля для доктора (roleId = 2)
        private String specialization;

        // Поля для пациента (roleId = 3)
        private String gender;
        private String address;

        // Общее поле для обоих
        private String contactNumber;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class LoginKeyRequest {
        private String username;
        private String privateKey;
    }
}