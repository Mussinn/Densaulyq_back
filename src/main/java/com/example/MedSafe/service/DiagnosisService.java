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
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisService.class);

    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final SecurityKeyRepository securityKeyRepository;

    @Transactional
    public Diagnosis createEncryptedDiagnosis(Integer patientId, Integer recordId, String diagnosisText) throws Exception {
        logger.info("Начало метода createEncryptedDiagnosis с параметрами: patientId={}, recordId={}, diagnosisText={}",
                patientId, recordId, diagnosisText);

        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient with ID " + patientId + " not found"));

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record with ID " + recordId + " not found"));

            SecurityKey securityKey = securityKeyRepository.findByUserUserId(patient.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Security key not found for user ID " + patient.getUser().getUserId()));

            logger.info("Перед шифрованием для patientId={}, recordId={}", patientId, recordId);
            String encryptedDiagnosis = encryptWithPublicKey(diagnosisText, securityKey.getPublicKey());

            Diagnosis diagnosis = new Diagnosis(record, encryptedDiagnosis, java.time.LocalDate.now());
            Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
            logger.info("Диагноз успешно сохранен: diagnosisId={}, encryptedDiagnosis={}", savedDiagnosis.getDiagnosisId(), savedDiagnosis.getDiagnosis());
            return savedDiagnosis;
        } catch (Exception e) {
            logger.error("Ошибка при создании зашифрованного диагноза: {}", e.getMessage(), e);
            throw e; // Передаем исключение дальше для обработки контроллером
        }
    }

    public Diagnosis createDefault(Integer patientId, Integer recordId, String diagnosisText) throws Exception {
        logger.info("Начало метода createEncryptedDiagnosis с параметрами: patientId={}, recordId={}, diagnosisText={}",
                patientId, recordId, diagnosisText);

        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient with ID " + patientId + " not found"));

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical record with ID " + recordId + " not found"));

            SecurityKey securityKey = securityKeyRepository.findByUserUserId(patient.getUser().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Security key not found for user ID " + patient.getUser().getUserId()));

            logger.info("Перед шифрованием для patientId={}, recordId={}", patientId, recordId);
//            String encryptedDiagnosis = encryptWithPublicKey(diagnosisText, securityKey.getPublicKey());

            Diagnosis diagnosis = new Diagnosis(record, diagnosisText, java.time.LocalDate.now());
            Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
            logger.info("Диагноз успешно сохранен: diagnosisId={}, encryptedDiagnosis={}", savedDiagnosis.getDiagnosisId(), savedDiagnosis.getDiagnosis());
            return savedDiagnosis;
        } catch (Exception e) {
            logger.error("Ошибка при создании зашифрованного диагноза: {}", e.getMessage(), e);
            throw e; // Передаем исключение дальше для обработки контроллером
        }
    }


    public String decryptDiagnosis(String encryptedDiagnosis, String privateKey) throws Exception {
        logger.info("Начало метода decryptDiagnosis с параметрами: encryptedDiagnosis={}, privateKey={}",
                encryptedDiagnosis, privateKey.substring(0, Math.min(50, privateKey.length())) + "...");

        try {
            if (encryptedDiagnosis == null || encryptedDiagnosis.trim().isEmpty()) {
                throw new IllegalArgumentException("Encrypted diagnosis cannot be null or empty");
            }
            if (privateKey == null || privateKey.trim().isEmpty()) {
                throw new IllegalArgumentException("Private key cannot be null or empty");
            }

            String decryptedText = decryptWithPrivateKey(encryptedDiagnosis, privateKey);
            logger.info("Диагноз успешно расшифрован: decryptedText={}", decryptedText.substring(0, Math.min(50, decryptedText.length())) + "...");
            return decryptedText;
        } catch (Exception e) {
            logger.error("Ошибка при расшифровке диагноза: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt diagnosis: " + e.getMessage(), e);
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

    private String encryptWithPublicKey(String plainText, String publicKeyStr) throws Exception {
        logger.debug("Начало шифрования с publicKeyStr={}", publicKeyStr.substring(0, Math.min(50, publicKeyStr.length())) + "...");
        try {
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(publicBytes));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

            String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
            logger.debug("Шифрование успешно: encryptedText={}", encryptedText.substring(0, Math.min(50, encryptedText.length())) + "...");
            return encryptedText;
        } catch (Exception e) {
            logger.error("Ошибка при шифровании: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String decryptWithPrivateKey(String encryptedText, String privateKeyStr) throws Exception {
        logger.debug("Начало расшифровки с privateKeyStr={}", privateKeyStr.substring(0, Math.min(50, privateKeyStr.length())) + "...");
        try {
            String privateKeyContent = privateKeyStr
                    .replaceAll("\\n", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .trim();

            if (!privateKeyContent.matches("^[A-Za-z0-9+/=]+$")) {
                throw new IllegalArgumentException("Некорректный формат приватного ключа");
            }

            byte[] privateBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privateBytes));

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));

            String decryptedText = new String(decryptedBytes);
            logger.debug("Расшифровка успешна: decryptedText={}", decryptedText.substring(0, Math.min(50, decryptedText.length())) + "...");
            return decryptedText;
        } catch (Exception e) {
            logger.error("Ошибка при расшифровке: {}", e.getMessage(), e);
            throw e;
        }
    }

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