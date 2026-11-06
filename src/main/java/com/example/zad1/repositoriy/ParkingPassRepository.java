package com.example.zad1.repositoriy;

import com.example.zad1.model.ParkingPass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParkingPassRepository extends JpaRepository<ParkingPass, Long> {
    List<ParkingPass> findByVehicle_Owner_Email(String email);
}