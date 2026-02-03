package com.example.MedSafe.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_history")
@Data
public class CallHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "call_id", unique = true)
    private String callId;

    @Column(name = "caller_id")
    private Long callerId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "call_type")
    private String callType;

    @Column(name = "status")
    private String status;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
