package com.example.MedSafe.controller;

import com.example.MedSafe.model.Diagnosis;
import com.example.MedSafe.service.DiagnosisService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosisController.class);
    private final DiagnosisService diagnosisService;

    @PostMapping("/create")
    public ResponseEntity<?> createDiagnosis(@RequestBody DiagnosisRequest request) {
        logger.info("Получен запрос на создание диагноза: patientId={}, recordId={}, diagnosisText={}",
                request.getPatientId(), request.getRecordId(), request.getDiagnosisText());

        try {
            Diagnosis diagnosis = diagnosisService.createEncryptedDiagnosis(
                    request.getPatientId(),
                    request.getRecordId(),
                    request.getDiagnosisText()
            );
            logger.info("Диагноз успешно создан: diagnosisId={}", diagnosis.getDiagnosisId());
            return ResponseEntity.ok(diagnosis);
        } catch (Exception e) {
            logger.error("Ошибка при создании диагноза: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create diagnosis: " + e.getMessage());
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<?> decryptDiagnosis(@RequestBody DecryptRequest request) {
        logger.info("Получен запрос на расшифровку: encryptedDiagnosis={}, privateKey={}",
                request.getEncryptedDiagnosis(), request.getPrivateKey().substring(0, Math.min(50, request.getPrivateKey().length())) + "...");

        try {
            String decryptedDiagnosis = diagnosisService.decryptDiagnosis(request.getEncryptedDiagnosis(), request.getPrivateKey());
            logger.info("Диагноз успешно расшифрован");
            return ResponseEntity.ok(decryptedDiagnosis);
        } catch (Exception e) {
            logger.error("Ошибка при расшифровке диагноза: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to decrypt diagnosis: " + e.getMessage());
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentDiagnoses() {
        logger.info("Получен запрос на получение последних диагнозов");
        try {
            List<Diagnosis> diagnoses = diagnosisService.getRecentDiagnoses();
            logger.info("Успешно возвращено {} последних диагнозов", diagnoses.size());
            return ResponseEntity.ok(diagnoses);
        } catch (Exception e) {
            logger.error("Ошибка при получении последних диагнозов: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve recent diagnoses: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getDiagnosesByUserId(@PathVariable Integer userId) {
        logger.info("Получен запрос на получение диагнозов для userId={}", userId);
        try {
            List<Diagnosis> diagnoses = diagnosisService.getByUserId(userId);
            logger.info("Успешно возвращено {} диагнозов для userId={}", diagnoses.size(), userId);
            return ResponseEntity.ok(diagnoses);
        } catch (Exception e) {
            logger.error("Ошибка при получении диагнозов для userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve diagnoses for userId " + userId + ": " + e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class DiagnosisRequest {
        private Integer patientId;
        private Integer recordId;
        private String diagnosisText;
    }

    @Getter
    @Setter
    public static class DecryptRequest {
        private String encryptedDiagnosis;
        private String privateKey;
    }
}