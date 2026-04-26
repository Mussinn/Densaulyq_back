package com.example.MedSafe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.MedSafe.model.Diagnosis;
import com.example.MedSafe.model.MedicalRecord;
import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.SecurityKey;
import com.example.MedSafe.repository.DiagnosisRepository;
import com.example.MedSafe.repository.MedicalRecordRepository;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.repository.SecurityKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisService.class);

    // Константы для AES-GCM шифрования
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // Константы для RSA
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";

    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final SecurityKeyRepository securityKeyRepository;

    @Transactional
    public Diagnosis createEncryptedDiagnosis(Integer patientId, Integer recordId, String diagnosisText) throws Exception {
        logger.info("Начало метода createEncryptedDiagnosis с параметрами: patientId={}, recordId={}, diagnosisTextLength={}",
                patientId, recordId, diagnosisText != null ? diagnosisText.length() : 0);

        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient with ID " + patientId + " not found"));

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record with ID " + recordId + " not found"));

            SecurityKey securityKey = securityKeyRepository.findByUserUserId(patient.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Security key not found for user ID " + patient.getUser().getUserId()));

            // Используем гибридное шифрование
            String encryptedDiagnosis = hybridEncrypt(diagnosisText, securityKey.getPublicKey());

            Diagnosis diagnosis = new Diagnosis(record, encryptedDiagnosis, java.time.LocalDate.now());
            Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
            logger.info("Диагноз успешно сохранен: diagnosisId={}", savedDiagnosis.getDiagnosisId());
            return savedDiagnosis;
        } catch (Exception e) {
            logger.error("Ошибка при создании зашифрованного диагноза: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Diagnosis createDefault(Integer patientId, Integer recordId, String diagnosisText) throws Exception {
        logger.info("Начало метода createDefault с параметрами: patientId={}, recordId={}, diagnosisTextLength={}",
                patientId, recordId, diagnosisText != null ? diagnosisText.length() : 0);

        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient with ID " + patientId + " not found"));

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record with ID " + recordId + " not found"));

            SecurityKey securityKey = securityKeyRepository.findByUserUserId(patient.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Security key not found for user ID " + patient.getUser().getUserId()));

            Diagnosis diagnosis = new Diagnosis(record, diagnosisText, java.time.LocalDate.now());
            Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
            logger.info("Диагноз успешно сохранен: diagnosisId={}", savedDiagnosis.getDiagnosisId());
            return savedDiagnosis;
        } catch (Exception e) {
            logger.error("Ошибка при создании диагноза: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Гибридное шифрование: AES-GCM для данных + RSA для шифрования AES ключа
     */
    private String hybridEncrypt(String plainText, String publicKeyStr) throws Exception {
        logger.debug("Начало гибридного шифрования");
        try {
            // 1. Генерируем случайный AES ключ
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            SecretKey aesKey = keyGen.generateKey();

            // 2. Генерируем случайный IV для GCM
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 3. Шифруем данные AES-GCM
            Cipher aesCipher = Cipher.getInstance(AES_CIPHER);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            byte[] encryptedData = aesCipher.doFinal(plainText.getBytes("UTF-8"));

            // 4. Шифруем AES ключ RSA публичным ключом
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicBytes));

            Cipher rsaCipher = Cipher.getInstance(RSA_CIPHER);
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            // 5. Формируем финальный пакет: [IV][Encrypted AES Key][Encrypted Data]
            byte[] result = new byte[iv.length + encryptedAesKey.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedAesKey, 0, result, iv.length, encryptedAesKey.length);
            System.arraycopy(encryptedData, 0, result, iv.length + encryptedAesKey.length, encryptedData.length);

            String encryptedResult = Base64.getEncoder().encodeToString(result);
            logger.debug("Гибридное шифрование успешно завершено");
            return encryptedResult;
        } catch (Exception e) {
            logger.error("Ошибка при гибридном шифровании: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String decryptDiagnosis(String encryptedDiagnosis, String privateKeyStr) throws Exception {
        logger.info("Начало метода decryptDiagnosis");
        try {
            if (encryptedDiagnosis == null || encryptedDiagnosis.trim().isEmpty()) {
                throw new IllegalArgumentException("Encrypted diagnosis cannot be null or empty");
            }
            if (privateKeyStr == null || privateKeyStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Private key cannot be null or empty");
            }

            String decryptedText = hybridDecrypt(encryptedDiagnosis, privateKeyStr);
            logger.info("Диагноз успешно расшифрован");
            return decryptedText;
        } catch (Exception e) {
            logger.error("Ошибка при расшифровке диагноза: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt diagnosis: " + e.getMessage(), e);
        }
    }

    /**
     * Гибридное дешифрование: расшифровка AES ключа через RSA и данных через AES-GCM
     */
    private String hybridDecrypt(String encryptedData, String privateKeyStr) throws Exception {
        logger.debug("Начало гибридного дешифрования");
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            // 1. Извлекаем IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);

            // 2. Извлекаем зашифрованный AES ключ
            // Определяем размер зашифрованного RSA ключа (обычно 256 байт для 2048 бит)
            int rsaKeyLength = 256; // Для RSA-2048
            byte[] encryptedAesKey = new byte[rsaKeyLength];
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, encryptedAesKey, 0, rsaKeyLength);

            // 3. Извлекаем зашифрованные данные
            byte[] encryptedDataBytes = new byte[encryptedBytes.length - GCM_IV_LENGTH - rsaKeyLength];
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH + rsaKeyLength, encryptedDataBytes, 0, encryptedDataBytes.length);

            // 4. Расшифровываем AES ключ приватным RSA ключом
            String privateKeyContent = privateKeyStr
                    .replaceAll("\\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .trim();

            if (!privateKeyContent.matches("^[A-Za-z0-9+/=]+$")) {
                throw new IllegalArgumentException("Некорректный формат приватного ключа");
            }

            byte[] privateBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateBytes));

            Cipher rsaCipher = Cipher.getInstance(RSA_CIPHER);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

            // 5. Расшифровываем данные AES-GCM
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher aesCipher = Cipher.getInstance(AES_CIPHER);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
            byte[] decryptedBytes = aesCipher.doFinal(encryptedDataBytes);

            String decryptedText = new String(decryptedBytes, "UTF-8");
            logger.debug("Гибридное дешифрование успешно завершено");
            return decryptedText;
        } catch (Exception e) {
            logger.error("Ошибка при гибридном дешифровании: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Diagnosis> getDiagnosesByPatientId(Integer patientId) {
        logger.info("Начало метода getDiagnosesByPatientId с параметром: patientId={}", patientId);
        try {
            List<Diagnosis> diagnoses = diagnosisRepository.findByMedicalRecord_Patient_PatientId(patientId);
            logger.info("Найдено {} диагнозов для patientId={}", diagnoses.size(), patientId);
            return diagnoses;
        } catch (Exception e) {
            logger.error("Ошибка при получении диагнозов для patientId={}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve diagnoses: " + e.getMessage(), e);
        }
    }

    public Diagnosis getDiagnosisById(Integer diagnosisId) {
        logger.info("Начало метода getDiagnosisById с параметром: diagnosisId={}", diagnosisId);
        try {
            Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                    .orElseThrow(() -> new IllegalArgumentException("Diagnosis with ID " + diagnosisId + " not found"));
            logger.info("Диагноз найден: diagnosisId={}", diagnosisId);
            return diagnosis;
        } catch (Exception e) {
            logger.error("Ошибка при получении диагноза с diagnosisId={}: {}", diagnosisId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve diagnosis: " + e.getMessage(), e);
        }
    }

    // Удаляем старые методы encryptWithPublicKey и decryptWithPrivateKey, так как они больше не нужны

    public List<Diagnosis> getRecentDiagnoses() {
        logger.info("Начало метода getRecentDiagnoses");
        try {
            List<Diagnosis> diagnoses = diagnosisRepository.findTop5ByOrderByCreatedAtDesc();
            logger.info("Найдено {} последних диагнозов", diagnoses.size());
            return diagnoses;
        } catch (Exception e) {
            logger.error("Ошибка при получении последних диагнозов: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve recent diagnoses: " + e.getMessage(), e);
        }
    }

    public List<Diagnosis> getByUserId(Integer userId) {
        logger.info("Начало метода getByUserId с параметром: userId={}", userId);
        try {
            List<Diagnosis> diagnoses = diagnosisRepository.findByUserId(userId);
            logger.info("Найдено {} диагнозов для userId={}", diagnoses.size(), userId);
            return diagnoses;
        } catch (Exception e) {
            logger.error("Ошибка при получении диагнозов для userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve diagnoses by userId: " + e.getMessage(), e);
        }
    }

    public List<Diagnosis> getByPatientId(Integer patientId) {
        logger.info("Начало метода getByPatientId с параметром: patientId={}", patientId);
        try {
            List<Diagnosis> diagnoses = diagnosisRepository.findByPatientId(patientId);
            logger.info("Найдено {} диагнозов для patientId={}", diagnoses.size(), patientId);
            return diagnoses;
        } catch (Exception e) {
            logger.error("Ошибка при получении диагнозов для patientId={}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve diagnoses by userId: " + e.getMessage(), e);
        }
    }
}