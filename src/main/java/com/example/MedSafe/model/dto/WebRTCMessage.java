package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebRTCMessage {
    private String callId;
    private String senderId;
    private String targetId;
    private Object data; // SDP offer/answer или ICE candidate
    private String type; // "offer", "answer", "candidate", "end"
    private Long timestamp;
}
