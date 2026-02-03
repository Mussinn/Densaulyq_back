package com.example.MedSafe.model.dto;

import lombok.Data;

@Data
public class CallRequest {
    private String callId;
    private String callerId;
    private String callerName;
    private String targetId;
    private String callerType; // "PATIENT" или "DOCTOR"
    private Long timestamp;
}
