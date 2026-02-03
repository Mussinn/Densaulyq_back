package com.example.MedSafe.controller;

import com.example.MedSafe.model.AuditLog;
import com.example.MedSafe.model.User;
import com.example.MedSafe.service.AuditService;
import lombok.*;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    // Логирование действия пользователя
    @PostMapping("/log")
    public void log(@RequestBody AuditRequest request) {
        auditService.log(
                request.getUser(),
                request.getAction(),
                request.getTargetTable(),
                request.getTargetId(),
                request.getIpAddress()
        );
    }

    // Получение всех логов
    @GetMapping("/logs")
    public List<AuditLog> getAuditLogs() {
        return auditService.getAuditLogs();
    }

    // Получение логов по userId
    @GetMapping("/user/{userId}")
    public List<AuditLog> getLogsByUser(@PathVariable Integer userId) {
        return auditService.getAuditLogsByUserId(userId);
    }

    @Getter @Setter
    public static class AuditRequest {
        private User user;
        private String action;
        private String targetTable;
        private Integer targetId;
        private String ipAddress;
    }
}

