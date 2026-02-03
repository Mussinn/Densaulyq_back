package com.example.MedSafe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer auditId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String action;

    private String targetTable;

    private Integer targetId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionDate = LocalDateTime.now();

    private String ipAddress;
}
