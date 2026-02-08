package com.example.MedSafe.controller;

import com.example.MedSafe.model.Test;
import com.example.MedSafe.model.dto.TestCreateRequest;
import com.example.MedSafe.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Medical Tests", description = "API для управления медицинскими тестами и анализами")
public class TestController {

    private final TestService testService;

    @Operation(summary = "Создать новый тест/анализ")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Test> createTest(
            @RequestParam("patientId") Integer patientId,
            @RequestParam("testName") String testName,
            @RequestParam("result") String result,
            @RequestParam(value = "testDate", required = false) String testDateStr,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            TestCreateRequest request = new TestCreateRequest();
            request.setPatientId(patientId);
            request.setTestName(testName);
            request.setResult(result);

            // Парсим дату
            if (testDateStr != null && !testDateStr.trim().isEmpty()) {
                LocalDateTime testDate;

                // Если есть Z в конце, убираем его и парсим как UTC
                if (testDateStr.endsWith("Z")) {
                    testDate = LocalDateTime.parse(
                            testDateStr.substring(0, testDateStr.length() - 1),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    );
                } else {
                    // Иначе парсим как есть
                    testDate = LocalDateTime.parse(testDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }

                request.setTestDate(testDate);
            }

            request.setFile(file);
            request.setImage(image);

            Test createdTest = testService.createTest(request);
            log.info("Тест создан успешно: ID={}, Пациент={}",
                    createdTest.getTestId(), patientId);

            return ResponseEntity.ok(createdTest);
        } catch (Exception e) {
            log.error("Ошибка создания теста для пациента {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Получить все тесты пациента")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Test>> getTestsByPatientId(@PathVariable Integer patientId) {
        try {
            List<Test> tests = testService.getTestsByPatientId(patientId);
            return ResponseEntity.ok(tests);
        } catch (Exception e) {
            log.error("Ошибка получения тестов для пациента {}: {}", patientId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Получить тест по ID")
    @GetMapping("/{testId}")
    public ResponseEntity<Test> getTestById(@PathVariable Integer testId) {
        try {
            Test test = testService.getTestById(testId);
            return ResponseEntity.ok(test);
        } catch (RuntimeException e) {
            log.error("Тест не найден: {}", testId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(summary = "Удалить тест")
    @DeleteMapping("/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Integer testId) {
        try {
            testService.deleteTest(testId);
            log.info("Тест удален: {}", testId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Ошибка удаления теста {}: {}", testId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Обновить тест")
    @PutMapping(value = "/{testId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Test> updateTest(
            @PathVariable Integer testId,
            @RequestParam(value = "testName", required = false) String testName,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            // Получаем существующий тест
            Test existingTest = testService.getTestById(testId);

            // Обновляем поля
            if (testName != null) existingTest.setTestName(testName);
            if (result != null) existingTest.setResult(result);

            // Обновляем файлы если предоставлены
            // Для простоты можно добавить метод updateTest в сервисе

            return ResponseEntity.ok(existingTest);
        } catch (Exception e) {
            log.error("Ошибка обновления теста {}: {}", testId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}