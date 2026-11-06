package com.example.zad1.config;

import com.example.zad1.model.User;
import com.example.zad1.repositoriy.ParkingPassRepository;
import com.example.zad1.repositoriy.ParkingSessionRepository;
import com.example.zad1.repositoriy.UserRepository;
import com.example.zad1.repositoriy.VehicleRepository;
import com.example.zad1.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Используется в аннотациях: @PreAuthorize("@access.ownsVehicle(#vehicleId)").
 * ADMIN проходит все проверки.
 * Если объекта нет - возвращаем true, и сервис ответит 404.
 */
@Component("access")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessChecker {

    private final CurrentUserService currentUser;
    private final VehicleRepository vehicleRepo;
    private final ParkingSessionRepository sessionRepo;
    private final ParkingPassRepository passRepo;
    private final UserRepository userRepo;

    public boolean ownsVehicle(Long vehicleId) {
        if (vehicleId == null || currentUser.isAdmin()) {
            return true;
        }
        return vehicleRepo.findById(vehicleId)
                .map(v -> isMe(v.getOwner()))
                .orElse(true);
    }

    public boolean ownsSession(Long sessionId) {
        if (currentUser.isAdmin()) {
            return true;
        }
        return sessionRepo.findById(sessionId)
                .map(s -> s.getVehicle() != null && isMe(s.getVehicle().getOwner()))
                .orElse(true);
    }

    public boolean ownsPass(Long passId) {
        if (currentUser.isAdmin()) {
            return true;
        }
        return passRepo.findById(passId)
                .map(p -> p.getVehicle() != null && isMe(p.getVehicle().getOwner()))
                .orElse(true);
    }

    public boolean isSelf(Long userId) {
        if (currentUser.isAdmin()) {
            return true;
        }
        return userRepo.findById(userId)
                .map(this::isMe)
                .orElse(true);
    }

    private boolean isMe(User user) {
        return user != null && user.getEmail().equals(currentUser.email());
    }
}