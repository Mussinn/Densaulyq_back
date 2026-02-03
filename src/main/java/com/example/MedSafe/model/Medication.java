package com.example.MedSafe.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer medicationId;

    @Column(nullable = false)
    private String name;

    private String dosage;

    private String instructions;
}
