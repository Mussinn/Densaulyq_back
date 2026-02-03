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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        User user = authService.registerUser(
                request.getUsername(), request.getPassword(), request.getEmail(),
                request.getFirstName(), request.getLastName(), request.getRoleId());

        auditService.log(
                user, "REGISTER", "users", user.getUserId(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        User user = authService.loginUser(request.getUsername(), request.getPassword());

        auditService.log(
                user, "LOGIN", "users", user.getUserId(), httpRequest.getRemoteAddr());
        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse authResponse = new AuthResponse(user.getUserId(), user.getUsername(), token, user.getRoles());
        return ResponseEntity.ok(authResponse);
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