package com.example.zad1.controller;

import com.example.zad1.model.*;
import com.example.zad1.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService service;

    // --- АВТОМОБИЛИ (владелец - текущий пользователь) ---

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle v) {
        Vehicle created = service.createVehicle(v);
        return ResponseEntity.created(URI.create("/api/vehicles/" + created.getId())).body(created);
    }

    // USER видит только свои машины, ADMIN - все
    @GetMapping("/vehicles")
    public List<Vehicle> getVehicles() {
        return service.getAllVehicles();
    }

    // --- ЗОНЫ ---

    @PostMapping("/parking-zones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingZone> createZone(@RequestBody ParkingZone z) {
        ParkingZone created = service.createZone(z);
        return ResponseEntity.created(URI.create("/api/parking-zones/" + created.getId())).body(created);
    }

    @GetMapping("/parking-zones")
    public List<ParkingZone> getZones() {
        return service.getAllZones();
    }

    // --- СЕССИИ ---

    @PostMapping("/parking-sessions/start")
    @PreAuthorize("@access.ownsVehicle(#vehicleId)")
    public ResponseEntity<ParkingSession> start(@RequestParam Long vehicleId,
                                                @RequestParam Long zoneId) {
        return ResponseEntity.status(201).body(service.startSession(vehicleId, zoneId));
    }

    @PatchMapping("/parking-sessions/{id}/end")
    @PreAuthorize("@access.ownsSession(#id)")
    public ResponseEntity<ParkingSession> end(@PathVariable Long id) {
        return ResponseEntity.ok(service.endSession(id));
    }

    @GetMapping("/parking-sessions")
    public List<ParkingSession> getSessions() {
        return service.getAllSessions();
    }

    // --- АБОНЕМЕНТЫ ---

    @PostMapping("/parking-passes/buy")
    @PreAuthorize("@access.ownsVehicle(#vehicleId)")
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
    // === БИЗНЕС-ОПЕРАЦИИ ===
    // ==========================================

    // 1. Привязать авто к абонементу: и абонемент, и машина должны быть своими
    @PatchMapping("/parking-passes/{passId}/register-vehicle")
    @PreAuthorize("@access.ownsPass(#passId) and @access.ownsVehicle(#vehicleId)")
    public ResponseEntity<ParkingPass> registerVehicleOnPass(@PathVariable Long passId,
                                                             @RequestParam Long vehicleId) {
        return ResponseEntity.ok(service.registerVehicleOnPass(passId, vehicleId));
    }

    // 2. Продлить абонемент
    @PatchMapping("/parking-passes/{passId}/extend")
    @PreAuthorize("@access.ownsPass(#passId)")
    public ResponseEntity<Map<String, Object>> extendPass(@PathVariable Long passId,
                                                          @RequestParam int days) {
        return ResponseEntity.ok(service.extendPass(passId, days));
    }

    // 3. Перенести сессию в другую зону
    @PostMapping("/parking-sessions/{sessionId}/move")
    @PreAuthorize("@access.ownsSession(#sessionId)")
    public ResponseEntity<Map<String, Object>> moveSessionToZone(@PathVariable Long sessionId,
                                                                 @RequestParam Long newZoneId) {
        return ResponseEntity.ok(service.moveSessionToZone(sessionId, newZoneId));
    }

    // 4. Возврат за абонемент
    @PatchMapping("/parking-passes/{passId}/refund")
    @PreAuthorize("@access.ownsPass(#passId)")
    public ResponseEntity<Map<String, Object>> refundPass(@PathVariable Long passId) {
        return ResponseEntity.ok(service.refundPass(passId));
    }

    // 5. Статистика: только своя (ADMIN - любая)
    @GetMapping("/users/{userId}/stats")
    @PreAuthorize("@access.isSelf(#userId)")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserStats(userId));
    }
}