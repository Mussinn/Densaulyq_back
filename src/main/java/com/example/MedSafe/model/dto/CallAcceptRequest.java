package com.example.MedSafe.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallAcceptRequest {
    private String callId;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private Long timestamp;
}
