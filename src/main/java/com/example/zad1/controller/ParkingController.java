package com.example.zad1.controller;

import com.example.zad1.model.*;
import com.example.zad1.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map; // Импортируем Map для ответов бизнес-операций

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService service;

    // ==========================================
    // === СТАНДАРТНЫЕ CRUD ОПЕРАЦИИ (Задание 2) ===
    // ==========================================

    // --- ПОЛЬЗОВАТЕЛИ ---

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = service.createUser(user);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return service.getAllUsers();
    }

    // --- АВТОМОБИЛИ ---

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle v) {
        Vehicle created = service.createVehicle(v);
        return ResponseEntity.created(URI.create("/api/vehicles/" + created.getId())).body(created);
    }

    @GetMapping("/vehicles")
    public List<Vehicle> getVehicles() {
        return service.getAllVehicles();
    }

    // --- ПАРКОВОЧНЫЕ ЗОНЫ ---

    @PostMapping("/parking-zones")
    public ResponseEntity<ParkingZone> createZone(@RequestBody ParkingZone z) {
        ParkingZone created = service.createZone(z);
        return ResponseEntity.created(URI.create("/api/parking-zones/" + created.getId())).body(created);
    }

    @GetMapping("/parking-zones")
    public List<ParkingZone> getZones() {
        return service.getAllZones();
    }

    // --- ПАРКОВОЧНЫЕ СЕССИИ ---

    @PostMapping("/parking-sessions/start")
    public ResponseEntity<ParkingSession> start(@RequestParam Long vehicleId,
                                                @RequestParam Long zoneId) {
        ParkingSession s = service.startSession(vehicleId, zoneId);
        return ResponseEntity.status(201).body(s);
    }

    @PatchMapping("/parking-sessions/{id}/end")
    public ResponseEntity<ParkingSession> end(@PathVariable Long id) {
        return ResponseEntity.ok(service.endSession(id));
    }

    @GetMapping("/parking-sessions")
    public List<ParkingSession> getSessions() {
        return service.getAllSessions();
    }

    // --- АБОНЕМЕНТЫ ---

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


    // ==========================================
    // === БИЗНЕС-ОПЕРАЦИИ (Задание 3) ===
    // ==========================================

    /**
     * Операция 1: Привязать авто к абонементу
     */
    @PatchMapping("/parking-passes/{passId}/register-vehicle")
    public ResponseEntity<ParkingPass> registerVehicleOnPass(
            @PathVariable Long passId,
            @RequestParam Long vehicleId) {
        ParkingPass updatedPass = service.registerVehicleOnPass(passId, vehicleId);
        return ResponseEntity.ok(updatedPass);
    }

    /**
     * Операция 2: Продлить абонемент
     */
    @PatchMapping("/parking-passes/{passId}/extend")
    public ResponseEntity<Map<String, Object>> extendPass(
            @PathVariable Long passId,
            @RequestParam int days) {
        Map<String, Object> result = service.extendPass(passId, days);
        return ResponseEntity.ok(result);
    }

    /**
     * Операция 3: Перенести сессию в другую зону
     */
    @PostMapping("/parking-sessions/{sessionId}/move")
    public ResponseEntity<Map<String, Object>> moveSessionToZone(
            @PathVariable Long sessionId,
            @RequestParam Long newZoneId) {
        Map<String, Object> result = service.moveSessionToZone(sessionId, newZoneId);
        return ResponseEntity.ok(result);
    }

    /**
     * Операция 4: Оформить возврат за абонемент
     */
    @PatchMapping("/parking-passes/{passId}/refund")
    public ResponseEntity<Map<String, Object>> refundPass(@PathVariable Long passId) {
        Map<String, Object> result = service.refundPass(passId);
        return ResponseEntity.ok(result);
    }

    /**
     * Операция 5: Статистика пользователя
     */
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        Map<String, Object> stats = service.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}