package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CallResponse {
    private String callId;
    private String status; // "accepted", "rejected", "ended", "missed"
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String reason;
    private Long timestamp;
}
