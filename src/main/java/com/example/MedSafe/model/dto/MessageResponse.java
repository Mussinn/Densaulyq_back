package com.example.MedSafe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Integer senderId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private ReplyToInfo replyTo;
    private List<AttachmentInfo> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyToInfo {
        private Long id;
        private Integer senderId;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentInfo {
        private Long id;
        private String name;
        private String url;
        private String type;
        private Long size;
    }
}