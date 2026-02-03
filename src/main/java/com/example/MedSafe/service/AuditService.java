package com.example.MedSafe.service;

import com.example.MedSafe.model.AuditLog;
import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String targetTable, Integer targetId, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setTargetTable(targetTable);
        log.setTargetId(targetId);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> getAuditLogsByUserId(Integer userId) {
        return auditLogRepository.findByUser_UserId(userId);
    }
}

