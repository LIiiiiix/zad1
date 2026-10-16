package com.example.zad1.service;

import com.example.zad1.model.*;
import com.example.zad1.repositoriy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {

    // Подключаем все репозитории
    private final UserRepository userRepo;
    private final VehicleRepository vehicleRepo;
    private final ParkingZoneRepository zoneRepo;
    private final ParkingSessionRepository sessionRepo;
    private final ParkingPassRepository passRepo;

    // --- ПОЛЬЗОВАТЕЛИ И АВТО (простое сохранение) ---

    public User createUser(User user) {
        return userRepo.save(user);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public Vehicle createVehicle(Vehicle v) {
        return vehicleRepo.save(v);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepo.findAll();
    }

    public ParkingZone createZone(ParkingZone z) {
        return zoneRepo.save(z);
    }

    public List<ParkingZone> getAllZones() {
        return zoneRepo.findAll();
    }

    // --- БИЗНЕС-ЛОГИКА ПАРКОВКИ ---

    // 1. Начать парковку
    public ParkingSession startSession(Long vehicleId, Long zoneId) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Авто не найдено"));
        ParkingZone zone = zoneRepo.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Зона не найдена"));

        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicle);
        session.setZone(zone);
        session.setEntryTime(LocalDateTime.now());
        session.setActive(true);
        session.setTotalAmount(BigDecimal.ZERO);

        return sessionRepo.save(session);
    }

    // 2. Завершить парковку (расчёт стоимости)
    public ParkingSession endSession(Long sessionId) {
        ParkingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));

        if (!session.isActive()) {
            throw new RuntimeException("Сессия уже завершена");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setExitTime(now);
        session.setActive(false);

        // считаем часы
        long hours = Duration.between(session.getEntryTime(), now).toHours();
        if (hours == 0) hours = 1;

        // считаем деньги: часы * цена зоны
        BigDecimal cost = session.getZone().getHourlyRate()
                .multiply(BigDecimal.valueOf(hours));
        session.setTotalAmount(cost);

        return sessionRepo.save(session);
    }

    // 3. Купить абонемент
    public ParkingPass buyPass(Long vehicleId, Long zoneId, int days) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Авто не найдено"));
        ParkingZone zone = zoneRepo.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Зона не найдена"));

        ParkingPass pass = new ParkingPass();
        pass.setVehicle(vehicle);
        pass.setZone(zone);
        pass.setStartDate(LocalDate.now());
        pass.setEndDate(LocalDate.now().plusDays(days));
        pass.setActive(true);

        return passRepo.save(pass);
    }

    // Списки всех сессий и абонементов
    public List<ParkingSession> getAllSessions() {
        return sessionRepo.findAll();
    }

    public List<ParkingPass> getAllPasses() {
        return passRepo.findAll();
    }
}