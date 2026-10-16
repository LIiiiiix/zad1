package com.example.zad1.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class ParkingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private ParkingZone zone;

    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private BigDecimal totalAmount;
    private boolean active;
}
