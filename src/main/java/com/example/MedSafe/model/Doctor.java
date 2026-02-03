package com.example.MedSafe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer doctorId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String specialty;

    private String contactNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Doctor(User user, String specialty, String contactNumber, LocalDateTime createdAt) {
        this.user = user;
        this.specialty = specialty;
        this.contactNumber = contactNumber;
        this.createdAt = createdAt;
    }
}

