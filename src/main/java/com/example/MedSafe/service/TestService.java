package com.example.MedSafe.service;

import com.example.MedSafe.model.Patient;
import com.example.MedSafe.model.Test;
import com.example.MedSafe.model.dto.TestCreateRequest;
import com.example.MedSafe.repository.PatientRepository;
import com.example.MedSafe.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final PatientRepository patientRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private String serverPort;

    // URL для доступа к файлам (можно настроить в application.properties)
    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public Test createTest(TestCreateRequest request) throws IOException {
        // Проверяем существование пациента
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Пациент не найден с ID: " + request.getPatientId()));

        // Создаем папки если не существуют
        createUploadDirectories();

        // Сохраняем файлы
        String fileUrl = null;
        String imageUrl = null;

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            fileUrl = saveFile(request.getFile(), "documents");
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            imageUrl = saveFile(request.getImage(), "images");
        }

        // Создаем тест
        Test test = new Test();
        test.setTestName(request.getTestName());
        test.setResult(request.getResult());
        test.setTestDate(request.getTestDate() != null ? request.getTestDate() : LocalDateTime.now());
        test.setPatient(patient);
        test.setFileUrl(fileUrl);
        test.setImageUrl(imageUrl);
        test.setCreatedAt(LocalDateTime.now());

        Test savedTest = testRepository.save(test);
        log.info("Тест создан успешно: ID={}, FileURL={}, ImageURL={}",
                savedTest.getTestId(), fileUrl, imageUrl);

        return savedTest;
    }

    private void createUploadDirectories() throws IOException {
        Path rootPath = Paths.get(uploadDir);
        Path documentsPath = rootPath.resolve("documents");
        Path imagesPath = rootPath.resolve("images");

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            log.info("Создана директория: {}", rootPath.toAbsolutePath());
        }
        if (!Files.exists(documentsPath)) {
            Files.createDirectories(documentsPath);
            log.info("Создана директория: {}", documentsPath.toAbsolutePath());
        }
        if (!Files.exists(imagesPath)) {
            Files.createDirectories(imagesPath);
            log.info("Создана директория: {}", imagesPath.toAbsolutePath());
        }
    }

    private String saveFile(MultipartFile file, String subdirectory) throws IOException {
        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Путь для сохранения
        Path targetPath = Paths.get(uploadDir, subdirectory, uniqueFilename);

        // Копируем файл
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Файл сохранен: {}", targetPath.toAbsolutePath());

        // Возвращаем URL для доступа к файлу через API
        return baseUrl + "/api/v1/files/" + subdirectory + "/" + uniqueFilename;
    }

    public List<Test> getTestsByPatientId(Integer patientId) {
        return testRepository.findByPatientPatientId(patientId);
    }

    public Test getTestById(Integer testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Тест не найден с ID: " + testId));
    }

    @Transactional
    public void deleteTest(Integer testId) throws IOException {
        Test test = getTestById(testId);

        // Удаляем файлы с диска
        if (test.getFileUrl() != null) {
            deleteFileFromUrl(test.getFileUrl());
        }
        if (test.getImageUrl() != null) {
            deleteFileFromUrl(test.getImageUrl());
        }

        testRepository.delete(test);
        log.info("Тест удален: ID={}", testId);
    }

    private void deleteFileFromUrl(String fileUrl) throws IOException {
        try {
            // Извлекаем путь файла из URL
            // Пример: http://localhost:8080/api/v1/files/documents/uuid.pdf
            String[] parts = fileUrl.split("/");
            if (parts.length < 2) {
                log.warn("Неверный формат URL файла: {}", fileUrl);
                return;
            }

            String filename = parts[parts.length - 1];
            String subdirectory = parts[parts.length - 2];

            Path filePath = Paths.get(uploadDir, subdirectory, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Файл удален: {}", filePath.toAbsolutePath());
            } else {
                log.warn("Файл не найден для удаления: {}", filePath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении файла: {}", fileUrl, e);
        }
    }

    @Transactional
    public Test updateTest(Integer testId, TestCreateRequest request) throws IOException {
        Test existingTest = getTestById(testId);

        // Обновляем текстовые поля
        if (request.getTestName() != null) {
            existingTest.setTestName(request.getTestName());
        }
        if (request.getResult() != null) {
            existingTest.setResult(request.getResult());
        }
        if (request.getTestDate() != null) {
            existingTest.setTestDate(request.getTestDate());
        }

        // Обновляем файлы если предоставлены
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            // Удаляем старый файл
            if (existingTest.getFileUrl() != null) {
                deleteFileFromUrl(existingTest.getFileUrl());
            }
            // Сохраняем новый
            String newFileUrl = saveFile(request.getFile(), "documents");
            existingTest.setFileUrl(newFileUrl);
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // Удаляем старое изображение
            if (existingTest.getImageUrl() != null) {
                deleteFileFromUrl(existingTest.getImageUrl());
            }
            // Сохраняем новое
            String newImageUrl = saveFile(request.getImage(), "images");
            existingTest.setImageUrl(newImageUrl);
        }

        return testRepository.save(existingTest);
    }
}