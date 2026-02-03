package com.example.MedSafe.controller;

import com.example.MedSafe.model.SecurityKey;
import com.example.MedSafe.model.User;
import com.example.MedSafe.model.dto.RsaGenerateRequest;
import com.example.MedSafe.service.SecurityKeyService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security-keys")
@RequiredArgsConstructor
public class SecurityKeyController {
    private final SecurityKeyService securityKeyService;

    // Генерация RSA ключей
    @PostMapping("/generate/{userId}")
    @PreAuthorize("isAuthenticated()")
    public SecurityKey generateKey(@PathVariable Integer userId) throws Exception {
        System.out.println("Generating keys for userId = " + userId);
        return securityKeyService.generateKeyPair(userId);
    }

    // Получение публичного ключа
    @GetMapping("/public/{userId}")
    @PreAuthorize("isAuthenticated()")
    public String getPublicKey(@PathVariable Integer userId) {
        return securityKeyService.getPublicKey(userId);
    }

    // Проверка валидности приватного ключа
    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public boolean validatePrivateKey(@RequestBody ValidatePrivateKeyRequest request) {
        return securityKeyService.validatePrivateKey(request.getPrivateKey(), request.getUserId());
    }

    @Getter
    @Setter
    public static class ValidatePrivateKeyRequest {
        private String privateKey;
        private Integer userId;
    }
}

