package com.example.zad1.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class ParkingPass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private ParkingZone zone;

    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
}