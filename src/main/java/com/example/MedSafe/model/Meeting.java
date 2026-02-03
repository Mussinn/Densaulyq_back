package com.example.MedSafe.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
@Data
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer appointmentId;

    @Column(nullable = false)
    private Integer doctorId;

    private Integer patientId;

    @Column(unique = true, nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String meetingUrl;

    private String topic;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String patientEmail;

    private LocalDateTime scheduledTime;
    private LocalDateTime createdAt = LocalDateTime.now();

    private Integer durationMinutes = 30;

}
