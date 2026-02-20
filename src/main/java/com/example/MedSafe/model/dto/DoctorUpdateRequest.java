package com.example.MedSafe.model.dto;

import lombok.Data;

@Data
public class DoctorUpdateRequest {
    private String specialty;
    private String contactNumber;
}
