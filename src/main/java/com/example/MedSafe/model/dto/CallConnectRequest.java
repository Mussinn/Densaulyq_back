package com.example.MedSafe.model.dto;

import lombok.Data;

@Data
public class CallConnectRequest {
    private String userId;
    private String sessionId;
    private String userRole;
    private String userName;
    private Long timestamp;
}
