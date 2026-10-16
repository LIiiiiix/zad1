package com.example.zad1.repositoriy;

import com.example.zad1.model.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {
}