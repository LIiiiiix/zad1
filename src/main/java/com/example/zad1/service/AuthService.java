package com.example.zad1.service;

import com.example.zad1.model.User;
import com.example.zad1.repositoriy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    // Регистрация: роль ВСЕГДА USER, выбрать её нельзя
    public User register(String name, String email, String rawPassword) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя обязательно");
        }
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Некорректный email");
        }
        checkPassword(rawPassword, email);
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRole(User.Role.USER);
        newUser.setStatus(User.Status.ACTIVE);
        return userRepo.save(newUser);
    }

    // Вызывается Spring Security при каждом входе
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())                               // -> ROLE_USER / ROLE_ADMIN
                .accountLocked(user.getStatus() == User.Status.BLOCKED)     // заблокированный -> 401
                .build();
    }

    // Политика надёжности пароля
    private void checkPassword(String password, String email) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Пароль должен быть не короче 8 символов");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Пароль должен быть не длиннее 72 байт");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new IllegalArgumentException(
                    "Пароль должен содержать заглавную и строчную буквы и цифру");
        }
        if (password.equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Пароль не должен совпадать с email");
        }
    }
}