package com.example.zad1.service;

import com.example.zad1.model.User;
import com.example.zad1.repositoriy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Кто выполняет текущий запрос (логин = email)
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepo;

    public String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Не выполнен вход");
        }
        return auth.getName();
    }

    public User get() {
        return userRepo.findByEmail(email())
                .orElseThrow(() -> new AccessDeniedException("Пользователь не найден"));
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}