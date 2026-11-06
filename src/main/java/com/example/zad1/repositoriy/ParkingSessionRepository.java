package com.example.zad1.repositoriy;

import com.example.zad1.model.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {    List<ParkingSession> findByVehicle_Owner_Email(String email);

    boolean existsByVehicle_IdAndActiveTrue(Long vehicleId);

    long countByZone_IdAndActiveTrue(Long zoneId);
}
