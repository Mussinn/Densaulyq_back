package com.example.MedSafe.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class TestCreateRequest {
    private Integer patientId;
    private String testName;
    private String result;
    private LocalDateTime testDate;
    private MultipartFile file;
    private MultipartFile image;
}
