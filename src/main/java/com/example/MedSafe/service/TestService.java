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

    @Value("${file.base-url:C:/bus/MedSafe}")
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

        return testRepository.save(test);
    }

//    private String saveImageToDisk(MultipartFile file) {
//        String uploadDir = UPLOAD_DIR;
//        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//        Path filePath = Paths.get(uploadDir + fileName);
//
//        try {
//            // Создаем директорию, если она не существует
//            Path directoryPath = Paths.get(uploadDir);
//            if (!Files.exists(directoryPath)) {
//                Files.createDirectories(directoryPath);
//            }
//
//            // Копируем файл в целевую директорию
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//            return filePath.toString().replace("\\", "/");
//
//        } catch (Exception e) {
//            logger.error("Ошибка при сохранении изображения на диск: {}", e.getMessage(), e);
//            throw new FailedToAddImageException("Не удалось сохранить изображение на диск.");
//        }
//    }

    private void createUploadDirectories() throws IOException {
        Path rootPath = Paths.get(uploadDir);
        Path documentsPath = rootPath.resolve("documents");
        Path imagesPath = rootPath.resolve("images");

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }
        if (!Files.exists(documentsPath)) {
            Files.createDirectories(documentsPath);
        }
        if (!Files.exists(imagesPath)) {
            Files.createDirectories(imagesPath);
        }
    }

    private String saveFile(MultipartFile file, String subdirectory) throws IOException {
        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Путь для сохранения
        Path targetPath = Paths.get(uploadDir, subdirectory, uniqueFilename);

        // Копируем файл
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Возвращаем URL для доступа к файлу
        return baseUrl + "/uploads/" + subdirectory + "/" + uniqueFilename;
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
    }

    private void deleteFileFromUrl(String fileUrl) throws IOException {
        // Извлекаем имя файла из URL
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // Определяем поддиректорию по типу файла
        String subdirectory = fileUrl.contains("/documents/") ? "documents" : "images";

        Path filePath = Paths.get(uploadDir, subdirectory, fileName);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Файл удален: {}", filePath);
        }
    }
}