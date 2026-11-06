package com.example.zad1.service;

import com.example.zad1.model.*;
import com.example.zad1.repositoriy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    // --- БИЗНЕС-ЛОГИКА ПАРКОВКИ (CRUD из задания 2) ---

    // 1. Начать парковку
    public ParkingSession startSession(Long vehicleId, Long zoneId) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Авто не найдено"));
        ParkingZone zone = zoneRepo.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Зона не найдена"));

        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicle);
        session.setZone(zone);
        session.setEntryTime(LocalDateTime.now()); // время ставит СЕРВЕР
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

        LocalDateTime now = LocalDateTime.now(); // время выхода - текущее
        session.setExitTime(now);
        session.setActive(false);

        // считаем часы
        long hours = Duration.between(session.getEntryTime(), now).toHours();
        if (hours == 0) hours = 1; // минимум 1 час

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


    // ==========================================
    // === НОВЫЕ БИЗНЕС-ОПЕРАЦИИ (ЗАДАНИЕ 3) ===
    // ==========================================

    /**
     * Операция 1: Зарегистрировать автомобиль на существующий абонемент.
     * Затрагивает: ParkingPass + Vehicle
     */
    public ParkingPass registerVehicleOnPass(Long passId, Long vehicleId) {
        ParkingPass pass = passRepo.findById(passId)
                .orElseThrow(() -> new RuntimeException("Абонемент не найден"));
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Авто не найдено"));

        if (!pass.isActive()) {
            throw new RuntimeException("Абонемент не активен");
        }

        // Проверка целостности: нет ли у этого авто другого активного абонемента в этой зоне?
        List<ParkingPass> activePassesForThisCarAndZone = passRepo.findAll().stream()
                .filter(p -> p.isActive()
                        && p.getVehicle() != null
                        && p.getVehicle().getId().equals(vehicleId)
                        && p.getZone().getId().equals(pass.getZone().getId()))
                .toList();

        // Если нашли хотя бы один другой (не тот, который редактируем), то ошибка
        boolean hasDuplicate = activePassesForThisCarAndZone.stream()
                .anyMatch(p -> !p.getId().equals(passId));

        if (hasDuplicate) {
            throw new RuntimeException("У авто уже есть активный абонемент в этой зоне");
        }

        pass.setVehicle(vehicle);
        return passRepo.save(pass);
    }

    /**
     * Операция 2: Продлить абонемент.
     * Затрагивает: ParkingPass + ParkingZone (для расчета цены)
     */
    public Map<String, Object> extendPass(Long passId, int days) {
        ParkingPass pass = passRepo.findById(passId)
                .orElseThrow(() -> new RuntimeException("Абонемент не найден"));

        if (!pass.isActive()) {
            throw new RuntimeException("Абонемент не активен");
        }

        LocalDate newEndDate = pass.getEndDate().plusDays(days);

        // Расчет стоимости продления (условно: дни * цена зоны за час * 24 часа)
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

    /**
     * Операция 3: Перенести парковочную сессию в другую зону.
     * Затрагивает: ParkingSession + ParkingZone (старая и новая)
     */
    public Map<String, Object> moveSessionToZone(Long sessionId, Long newZoneId) {
        ParkingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена"));
        ParkingZone newZone = zoneRepo.findById(newZoneId)
                .orElseThrow(() -> new RuntimeException("Новая зона не найдена"));

        if (!session.isActive()) {
            throw new RuntimeException("Сессия уже завершена");
        }

        // Проверка вместимости новой зоны
        long activeSessionsInNewZone = sessionRepo.findAll().stream()
                .filter(s -> s.isActive() && s.getZone().getId().equals(newZoneId))
                .count();

        if (activeSessionsInNewZone >= newZone.getCapacity()) {
            throw new RuntimeException("В новой зоне нет свободных мест");
        }

        // 1. Завершаем старую сессию (фиксируем стоимость по старой зоне)
        LocalDateTime now = LocalDateTime.now();
        session.setExitTime(now);
        session.setActive(false);

        long hoursOld = Duration.between(session.getEntryTime(), now).toHours();
        if (hoursOld == 0) hoursOld = 1; // минимум 1 час

        BigDecimal costOld = session.getZone().getHourlyRate()
                .multiply(BigDecimal.valueOf(hoursOld));
        session.setTotalAmount(costOld);
        sessionRepo.save(session);

        // 2. Создаем новую сессию в новой зоне
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

    /**
     * Операция 4: Оформить возврат за неиспользованный абонемент.
     * Затрагивает: ParkingPass + ParkingZone
     */
    public Map<String, Object> refundPass(Long passId) {
        ParkingPass pass = passRepo.findById(passId)
                .orElseThrow(() -> new RuntimeException("Абонемент не найден"));

        if (!pass.isActive()) {
            throw new RuntimeException("Абонемент уже не активен или удален");
        }

        LocalDate today = LocalDate.now();
        if (pass.getEndDate().isBefore(today)) {
            throw new RuntimeException("Срок абонемента истек, возврат невозможен");
        }

        // Считаем сколько дней осталось
        long remainingDays = ChronoUnit.DAYS.between(today, pass.getEndDate());

        // Возврат = остаток дней * суточная ставка (цена_за_час * 24)
        BigDecimal dailyRate = pass.getZone().getHourlyRate().multiply(BigDecimal.valueOf(24));
        BigDecimal refund = dailyRate.multiply(BigDecimal.valueOf(remainingDays));

        pass.setActive(false); // Деактивируем абонемент
        passRepo.save(pass);

        Map<String, Object> result = new HashMap<>();
        result.put("refundAmount", refund);
        result.put("remainingDays", remainingDays);
        result.put("status", "REFUNDED");
        return result;
    }

    /**
     * Операция 5: Получить статистику парковки по пользователю.
     * Затрагивает: User + Vehicle + ParkingSession + ParkingZone
     */
    public Map<String, Object> getUserStats(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Находим все машины пользователя
        List<Vehicle> vehicles = vehicleRepo.findAll().stream()
                .filter(v -> v.getOwner() != null && v.getOwner().getId().equals(userId))
                .toList();

        if (vehicles.isEmpty()) {
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("userName", user.getName());
            emptyStats.put("totalSessions", 0);
            emptyStats.put("totalSpent", BigDecimal.ZERO);
            emptyStats.put("visitedZones", Set.of());
            emptyStats.put("vehicleCount", 0);
            return emptyStats;
        }

        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();

        // Находим все ЗАВЕРШЕННЫЕ сессии этих машин
        List<ParkingSession> completedSessions = sessionRepo.findAll().stream()
                .filter(s -> !s.isActive() && vehicleIds.contains(s.getVehicle().getId()))
                .toList();

        // Суммируем расходы
        BigDecimal totalSpent = completedSessions.stream()
                .map(ParkingSession::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Уникальные зоны
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
}