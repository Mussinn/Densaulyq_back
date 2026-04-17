package com.example.MedSafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // URL для доступа к файлам (можно настроить в application.properties)
    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${file.messages-dir:uploads/messages}")
    private String messagesDir;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;


    public String saveFileV2(MultipartFile file) throws IOException {

        String subdirectory = "messages";
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

        // Возвращаем URL для доступа к файлу через API
        return baseUrl + "/api/v1/files/" + subdirectory + "/" + uniqueFilename;
    }

    public String saveFile(MultipartFile file) throws IOException {
        // Создаем директорию если не существует
        Path uploadPath = Paths.get(messagesDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Сохраняем файл
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(messagesDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Логируем ошибку, но не прерываем выполнение
            System.err.println("Failed to delete file: " + fileName);
        }
    }

    public String getFileUrl(String fileName) {
        return "/api/v1/files/" + fileName;
    }
}