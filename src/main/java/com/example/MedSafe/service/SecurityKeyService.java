package com.example.MedSafe.service;

import com.example.MedSafe.model.SecurityKey;
import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.SecurityKeyRepository;
import com.example.MedSafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SecurityKeyService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityKeyService.class);
    private final SecurityKeyRepository securityKeyRepository;
    private final UserRepository userRepository;

    public SecurityKey generateKeyPair(Integer userId) throws Exception {
        logger.info("Попытка генерации ключей для userId: {}", userId);

        // Проверка userId
        if (userId == null) {
            logger.error("Попытка генерации ключей с null userId");
            throw new IllegalArgumentException("UserId cannot be null");
        }

        // Поиск пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("Пользователь с id={} не найден", userId);
                    return new IllegalArgumentException("Пользователь с id=" + userId + " не найден");
                });
        logger.info("Найден пользователь: {}", user.getUsername());

        // Проверка существования ключей
        SecurityKey existingKey = securityKeyRepository.findByUserUserId(userId).orElse(null);
        if (existingKey != null) {
            logger.warn("Ключи для userId={} уже существуют. Повторная генерация запрещена.", userId);
            securityKeyRepository.delete(existingKey);
//            throw new IllegalStateException("Ключи для пользователя уже существуют. Повторная генерация невозможна, чтобы сохранить доступ к зашифрованным данным.");
        }

        // Генерация новой пары ключей
        logger.info("Генерация новой пары ключей для userId: {}", userId);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        // Создание нового объекта SecurityKey
        SecurityKey key = new SecurityKey();
        key.setUser(user);
        key.setPublicKey(publicKey);
        key.setPrivateKey(privateKey);

        // Сохранение ключа
        SecurityKey savedKey = securityKeyRepository.save(key);
        logger.info("Ключ успешно создан: userId={}, publicKey={}",
                savedKey.getUser().getUserId(), savedKey.getPublicKey());
        return savedKey;
    }

    public String getPublicKey(Integer userId) {
        logger.info("Получение публичного ключа для userId: {}", userId);
        SecurityKey key = securityKeyRepository.findByUserUserId(userId)
                .orElseThrow(() -> {
                    logger.error("Публичный ключ для userId {} не найден", userId);
                    return new RuntimeException("Публичный ключ не найден");
                });
        logger.info("Найден публичный ключ для userId: {}", userId);
        return key.getPublicKey();
    }

    public boolean validatePrivateKey(String privateKey, Integer userId) {
        logger.info("Валидация приватного ключа для userId: {}", userId);
        SecurityKey key = securityKeyRepository.findByUserUserId(userId)
                .orElseThrow(() -> {
                    logger.error("Ключ для userId {} не найден", userId);
                    return new RuntimeException("Ключ не найден");
                });
        boolean isValid = key.getPrivateKey().equals(privateKey);
        logger.info("Результат валидации ключа для userId {}: {}", userId, isValid);
        return isValid;
    }
}
