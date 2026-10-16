package com.example.zad1.controller;

import com.example.zad1.model.*;
import com.example.zad1.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService service;

    // === ПОЛЬЗОВАТЕЛИ ===

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = service.createUser(user);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return service.getAllUsers();
    }

    // === АВТОМОБИЛИ ===

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle v) {
        Vehicle created = service.createVehicle(v);
        return ResponseEntity.created(URI.create("/api/vehicles/" + created.getId())).body(created);
    }

    @GetMapping("/vehicles")
    public List<Vehicle> getVehicles() {
        return service.getAllVehicles();
    }

    // === ПАРКОВОЧНЫЕ ЗОНЫ ===

    @PostMapping("/parking-zones")
    public ResponseEntity<ParkingZone> createZone(@RequestBody ParkingZone z) {
        ParkingZone created = service.createZone(z);
        return ResponseEntity.created(URI.create("/api/parking-zones/" + created.getId())).body(created);
    }

    @GetMapping("/parking-zones")
    public List<ParkingZone> getZones() {
        return service.getAllZones();
    }

    // === ПАРКОВОЧНЫЕ СЕССИИ ===

    // Начать парковку: передаём ID машины и ID зоны
    @PostMapping("/parking-sessions/start")
    public ResponseEntity<ParkingSession> start(@RequestParam Long vehicleId,
                                                @RequestParam Long zoneId) {
        ParkingSession s = service.startSession(vehicleId, zoneId);
        return ResponseEntity.status(201).body(s);
    }

    // Завершить парковку (расчёт стоимости)
    @PatchMapping("/parking-sessions/{id}/end")
    public ResponseEntity<ParkingSession> end(@PathVariable Long id) {
        return ResponseEntity.ok(service.endSession(id));
    }

    @GetMapping("/parking-sessions")
    public List<ParkingSession> getSessions() {
        return service.getAllSessions();
    }

    // === АБОНЕМЕНТЫ ===

    @PostMapping("/parking-passes/buy")
    public ResponseEntity<ParkingPass> buyPass(@RequestParam Long vehicleId,
                                               @RequestParam Long zoneId,
                                               @RequestParam int days) {
        return ResponseEntity.status(201).body(service.buyPass(vehicleId, zoneId, days));
    }

    @GetMapping("/parking-passes")
    public List<ParkingPass> getPasses() {
        return service.getAllPasses();
    }
}
