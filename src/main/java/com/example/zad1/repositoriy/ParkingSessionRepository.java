package com.example.zad1.repositoriy;

import com.example.zad1.model.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
}
