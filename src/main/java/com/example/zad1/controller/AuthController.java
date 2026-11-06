package com.example.zad1.controller;

import com.example.zad1.model.User;
import com.example.zad1.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Регистрация: поле role в запросе отсутствует, роль всегда USER
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        User registered = authService.register(
                request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "userId", registered.getId(),
                "email", registered.getEmail(),
                "role", registered.getRole()));
    }

    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        // поля role здесь нет специально
    }
}