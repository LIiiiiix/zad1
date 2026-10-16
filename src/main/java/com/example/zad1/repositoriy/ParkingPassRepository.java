package com.example.zad1.repositoriy;

import com.example.zad1.model.ParkingPass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingPassRepository extends JpaRepository<ParkingPass, Long> {
}