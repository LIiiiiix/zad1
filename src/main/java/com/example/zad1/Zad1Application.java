package com.example.zad1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity // Включаем поддержку аннотаций безопасности (роли)
public class Zad1Application {

    public static void main(String[] args) {
        SpringApplication.run(Zad1Application.class, args);
    }
}