package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    private Integer doctorId;

    private Integer patientId;

    private LocalDateTime appointmentDate;

    private String reason;

    private String appointmentType = "CONSULTATION";

    private String status = "SCHEDULED";

    private String consultationPrice;
}
