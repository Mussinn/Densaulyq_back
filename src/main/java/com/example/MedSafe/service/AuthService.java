package com.example.MedSafe.service;

import com.example.MedSafe.model.*;
import com.example.MedSafe.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SecurityKeyRepository securityKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CallService callService;

    public User registerUser(
            String username,
            String password,
            String email,
            String firstName,
            String lastName,
            Integer roleId,
            String specialty,
            String contactNumber,
            String address,
            String gender) {

        logger.info("Регистрация пользователя: username={}, roleId={}", username, roleId);

        if (userRepository.existsByUsername(username)) {
            logger.error("Имя пользователя {} уже занято", username);
            throw new RuntimeException("Username already taken");
        }

        Role userRole = roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.error("Роль с ID {} не найдена", roleId);
                    return new RuntimeException("Role not found");
                });

        // Создаем основного пользователя
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setRoles(Collections.singleton(userRole));

        User savedUser = userRepository.save(user);
        logger.info("Пользователь сохранен: userId={}, roleId={}", savedUser.getUserId(), roleId);

        // Создаем дополнительную запись в зависимости от роли
        try {
            if (roleId == 2) {
                // Доктор
                logger.info("Создание записи доктора для userId={}", savedUser.getUserId());
                Doctor doctor = new Doctor(
                        savedUser,
                        specialty != null ? specialty : "Не указана",
                        contactNumber != null ? contactNumber : "",
                        LocalDateTime.now()
                );
                doctorRepository.save(doctor);
                logger.info("Запись доктора успешно создана");

            } else if (roleId == 3) {
                // Пациент
                logger.info("Создание записи пациента для userId={}", savedUser.getUserId());
                Patient patient = new Patient(
                        savedUser,
                        LocalDate.now(), // По умолчанию текущая дата, будет обновлена позже если нужно
                        gender != null ? gender : "other",
                        contactNumber != null ? contactNumber : "",
                        address != null ? address : "",
                        LocalDateTime.now()
                );
                patientRepository.save(patient);
                logger.info("Запись пациента успешно создана");
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании дополнительной записи для роли {}: {}", roleId, e.getMessage(), e);
            // Не прерываем регистрацию, основной пользователь уже создан
        }

        // Устанавливаем онлайн-статус
        try {
            markUserAsOnline(savedUser);
        } catch (Exception e) {
            logger.warn("Не удалось установить онлайн-статус при регистрации: {}", e.getMessage());
        }

        logger.info("Пользователь успешно зарегистрирован: userId={}", savedUser.getUserId());
        return savedUser;
    }

    public User loginUser(String username, String password) {
        logger.info("Попытка входа по паролю: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Пользователь {} не найден", username);
                    return new RuntimeException("User not found");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.error("Неверные учетные данные для пользователя {}", username);
            throw new RuntimeException("Invalid credentials");
        }

        try {
            markUserAsOnline(user);
        } catch (Exception e) {
            logger.warn("Не удалось установить онлайн-статус при входе: {}", e.getMessage());
        }

        logger.info("Успешный вход по паролю: userId={}", user.getUserId());
        return user;
    }

    public User loginWithPrivateKey(String username, String privateKey) throws Exception {
        logger.info("Попытка входа по приватному ключу: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Пользователь {} не найден", username);
                    return new RuntimeException("User not found");
                });

        SecurityKey securityKey = securityKeyRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> {
                    logger.error("Ключи для пользователя {} не найдены", username);
                    return new RuntimeException("Security key not found");
                });

        String challenge = UUID.randomUUID().toString();
        logger.debug("Сгенерировано случайное сообщение для подписи: {}", challenge);

        try {
            String privateKeyContent = privateKey
                    .replaceAll("\\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .trim();

            if (!privateKeyContent.matches("^[A-Za-z0-9+/=]+$")) {
                logger.error("Некорректный формат приватного ключа для пользователя {}", username);
                throw new IllegalArgumentException("Invalid private key format");
            }

            byte[] privateBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey parsedPrivateKey = keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateBytes));

            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(parsedPrivateKey);
            signer.update(challenge.getBytes());
            byte[] signature = signer.sign();

            byte[] publicBytes = Base64.getDecoder().decode(securityKey.getPublicKey());
            PublicKey publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicBytes));

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge.getBytes());
            boolean isValid = verifier.verify(signature);

            if (!isValid) {
                logger.error("Неверная подпись для пользователя {}", username);
                throw new RuntimeException("Invalid private key");
            }

            try {
                markUserAsOnline(user);
            } catch (Exception e) {
                logger.warn("Не удалось установить онлайн-статус при входе по ключу: {}", e.getMessage());
            }

            logger.info("Успешный вход по приватному ключу: userId={}", user.getUserId());
            return user;
        } catch (Exception e) {
            logger.error("Ошибка при проверке приватного ключа для пользователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to authenticate with private key: " + e.getMessage(), e);
        }
    }

    private void markUserAsOnline(User user) {
        try {
            String userId = user.getUserId().toString();
            String sessionId = "auth-session-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8);

            callService.registerUser(userId, sessionId);

            user.setOnline(true);
            userRepository.save(user);

            logger.info("Пользователь {} помечен как онлайн через AuthService", userId);

        } catch (Exception e) {
            logger.error("Ошибка при установке онлайн-статуса для пользователя {}: {}",
                    user.getUserId(), e.getMessage());
        }
    }

    public void logoutUser(Integer userId) {
        try {
            String userIdStr = userId.toString();

            callService.unregisterUser(userIdStr);

            userRepository.findById(userId).ifPresent(user -> {
                user.setOnline(false);
                userRepository.save(user);
                logger.info("Пользователь {} помечен как офлайн", userId);
            });

        } catch (Exception e) {
            logger.warn("Ошибка при выходе пользователя {}: {}", userId, e.getMessage());
        }
    }
}