package com.example.MedSafe.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetings_consilium")
@Data
public class MeetingConsilium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer senderDoctorId;

    private Integer receiverDoctorId;

    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String meetingUrl;

    private String topic;

    @Column(nullable = false)
    private String status;

    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer durationMinutes = 30;
}