package com.example.zad1.service;

import com.example.zad1.model.*;
import com.example.zad1.repositoriy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // каждая операция - одна транзакция: либо целиком, либо откат
public class ParkingService {

    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final ParkingZoneRepository zoneRepo;
    private final ParkingSessionRepository sessionRepo;
    private final ParkingPassRepository passRepo;
    private final CurrentUserService currentUser;

    // --- АВТОМОБИЛИ ---

    public Vehicle createVehicle(Vehicle v) {
        if (v.getPlateNumber() == null || v.getPlateNumber().isBlank()) {
            throw new IllegalArgumentException("Госномер обязателен");
        }
        v.setId(null);                  // нельзя перезаписать чужую запись, подсунув id
        v.setOwner(currentUser.get());  // владелец - тот, кто вошёл, а не тот, кого прислали
        return vehicleRepo.save(v);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return currentUser.isAdmin()
                ? vehicleRepo.findAll()
                : vehicleRepo.findByOwner_Email(currentUser.email());
    }

    // --- ЗОНЫ ---

    public ParkingZone createZone(ParkingZone z) {
        z.setId(null);
        return zoneRepo.save(z);
    }

    @Transactional(readOnly = true)
    public List<ParkingZone> getAllZones() {
        return zoneRepo.findAll();
    }

    // --- СЕССИИ ---

    public ParkingSession startSession(Long vehicleId, Long zoneId) {
        Vehicle vehicle = findVehicle(vehicleId);
        ParkingZone zone = findZone(zoneId);

        if (sessionRepo.existsByVehicle_IdAndActiveTrue(vehicleId)) {
            throw new IllegalStateException("У авто уже есть активная парковочная сессия");
        }
        if (sessionRepo.countByZone_IdAndActiveTrue(zoneId) >= zone.getCapacity()) {
            throw new IllegalStateException("В зоне нет свободных мест");
        }

        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicle);
        session.setZone(zone);
        session.setEntryTime(LocalDateTime.now());
        session.setActive(true);
        session.setTotalAmount(BigDecimal.ZERO);
        return sessionRepo.save(session);
    }

    public ParkingSession endSession(Long sessionId) {
        ParkingSession session = findSession(sessionId);
        if (!session.isActive()) {
            throw new IllegalStateException("Сессия уже завершена");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setExitTime(now);
        session.setActive(false);
        session.setTotalAmount(calculateCost(session, now));
        return sessionRepo.save(session);
    }

    @Transactional(readOnly = true)
    public List<ParkingSession> getAllSessions() {
        return currentUser.isAdmin()
                ? sessionRepo.findAll()
                : sessionRepo.findByVehicle_Owner_Email(currentUser.email());
    }

    // --- АБОНЕМЕНТЫ ---

    public ParkingPass buyPass(Long vehicleId, Long zoneId, int days) {
        checkDays(days);
        Vehicle vehicle = findVehicle(vehicleId);
        ParkingZone zone = findZone(zoneId);

        ParkingPass pass = new ParkingPass();
        pass.setVehicle(vehicle);
        pass.setZone(zone);
        pass.setStartDate(LocalDate.now());
        pass.setEndDate(LocalDate.now().plusDays(days));
        pass.setActive(true);
        return passRepo.save(pass);
    }

    @Transactional(readOnly = true)
    public List<ParkingPass> getAllPasses() {
        return currentUser.isAdmin()
                ? passRepo.findAll()
                : passRepo.findByVehicle_Owner_Email(currentUser.email());
    }

    // ==========================================
    // === БИЗНЕС-ОПЕРАЦИИ (ЗАДАНИЕ 3) ===
    // ==========================================

    // Операция 1: привязать авто к абонементу
    public ParkingPass registerVehicleOnPass(Long passId, Long vehicleId) {
        ParkingPass pass = findPass(passId);
        Vehicle vehicle = findVehicle(vehicleId);

        if (!pass.isActive()) {
            throw new IllegalStateException("Абонемент не активен");
        }

        boolean hasDuplicate = passRepo.findAll().stream()
                .filter(p -> p.isActive()
                        && p.getVehicle() != null
                        && p.getVehicle().getId().equals(vehicleId)
                        && p.getZone().getId().equals(pass.getZone().getId()))
                .anyMatch(p -> !p.getId().equals(passId));
        if (hasDuplicate) {
            throw new IllegalStateException("У авто уже есть активный абонемент в этой зоне");
        }

        pass.setVehicle(vehicle);
        return passRepo.save(pass);
    }

    // Операция 2: продлить абонемент
    public Map<String, Object> extendPass(Long passId, int days) {
        checkDays(days);
        ParkingPass pass = findPass(passId);
        if (!pass.isActive()) {
            throw new IllegalStateException("Абонемент не активен");
        }

        LocalDate newEndDate = pass.getEndDate().plusDays(days);
        BigDecimal cost = pass.getZone().getHourlyRate()
                .multiply(BigDecimal.valueOf(days))
                .multiply(BigDecimal.valueOf(24));

        pass.setEndDate(newEndDate);
        passRepo.save(pass);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Абонемент успешно продлен");
        result.put("newEndDate", newEndDate.toString());
        result.put("extensionCost", cost);
        return result;
    }

    // Операция 3: перенести сессию в другую зону (две записи - одна транзакция)
    public Map<String, Object> moveSessionToZone(Long sessionId, Long newZoneId) {
        ParkingSession session = findSession(sessionId);
        ParkingZone newZone = findZone(newZoneId);

        if (!session.isActive()) {
            throw new IllegalStateException("Сессия уже завершена");
        }
        if (session.getZone().getId().equals(newZoneId)) {
            throw new IllegalStateException("Машина уже находится в этой зоне");
        }
        if (sessionRepo.countByZone_IdAndActiveTrue(newZoneId) >= newZone.getCapacity()) {
            throw new IllegalStateException("В новой зоне нет свободных мест");
        }

        // 1. Завершаем старую сессию
        LocalDateTime now = LocalDateTime.now();
        session.setExitTime(now);
        session.setActive(false);
        BigDecimal costOld = calculateCost(session, now);
        session.setTotalAmount(costOld);
        sessionRepo.save(session);

        // 2. Создаём новую в новой зоне
        ParkingSession newSession = new ParkingSession();
        newSession.setVehicle(session.getVehicle());
        newSession.setZone(newZone);
        newSession.setEntryTime(now);
        newSession.setActive(true);
        newSession.setTotalAmount(BigDecimal.ZERO);
        sessionRepo.save(newSession);

        Map<String, Object> result = new HashMap<>();
        result.put("oldSessionId", session.getId());
        result.put("oldSessionCost", costOld);
        result.put("newSessionId", newSession.getId());
        result.put("message", "Машина перемещена в зону: " + newZone.getName());
        return result;
    }

    // Операция 4: возврат за неиспользованный абонемент
    public Map<String, Object> refundPass(Long passId) {
        ParkingPass pass = findPass(passId);
        if (!pass.isActive()) {
            throw new IllegalStateException("Абонемент уже не активен");
        }

        LocalDate today = LocalDate.now();
        if (pass.getEndDate().isBefore(today)) {
            throw new IllegalStateException("Срок абонемента истек, возврат невозможен");
        }

        long remainingDays = ChronoUnit.DAYS.between(today, pass.getEndDate());
        BigDecimal dailyRate = pass.getZone().getHourlyRate().multiply(BigDecimal.valueOf(24));
        BigDecimal refund = dailyRate.multiply(BigDecimal.valueOf(remainingDays));

        pass.setActive(false);
        passRepo.save(pass);

        Map<String, Object> result = new HashMap<>();
        result.put("refundAmount", refund);
        result.put("remainingDays", remainingDays);
        result.put("status", "REFUNDED");
        return result;
    }

    // Операция 5: статистика пользователя
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден"));

        List<Vehicle> vehicles = vehicleRepo.findAll().stream()
                .filter(v -> v.getOwner() != null && v.getOwner().getId().equals(userId))
                .toList();
        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();

        List<ParkingSession> completedSessions = sessionRepo.findAll().stream()
                .filter(s -> !s.isActive() && vehicleIds.contains(s.getVehicle().getId()))
                .toList();

        BigDecimal totalSpent = completedSessions.stream()
                .map(ParkingSession::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<String> visitedZones = completedSessions.stream()
                .map(s -> s.getZone().getName())
                .collect(Collectors.toSet());

        Map<String, Object> stats = new HashMap<>();
        stats.put("userName", user.getName());
        stats.put("totalSessions", completedSessions.size());
        stats.put("totalSpent", totalSpent);
        stats.put("visitedZones", visitedZones);
        stats.put("vehicleCount", vehicles.size());
        return stats;
    }

    // --- вспомогательные ---

    private BigDecimal calculateCost(ParkingSession session, LocalDateTime exit) {
        long hours = Duration.between(session.getEntryTime(), exit).toHours();
        if (hours == 0) {
            hours = 1; // минимум 1 час
        }
        return session.getZone().getHourlyRate().multiply(BigDecimal.valueOf(hours));
    }

    private void checkDays(int days) {
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("Количество дней должно быть от 1 до 365");
        }
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Авто не найдено"));
    }

    private ParkingZone findZone(Long id) {
        return zoneRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Зона не найдена"));
    }

    private ParkingSession findSession(Long id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Сессия не найдена"));
    }

    private ParkingPass findPass(Long id) {
        return passRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Абонемент не найден"));
    }
}