package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallNotification {
    private String callId;
    private String callerId;
    private String callerName;
    private String callerType;
    private String type; // "incoming", "missed", "ended"
    private Long timestamp;
}
