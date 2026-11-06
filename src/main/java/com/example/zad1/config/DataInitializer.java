package com.example.zad1.config;

import com.example.zad1.model.User;
import com.example.zad1.repositoriy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component // Аннотация Component позволяет Spring запустить этот бин при старте
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepositoriy;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, есть ли уже пользователи. Если нет - создаем.
        if (userRepositoriy.count() == 0) {

            // 1. Создаем МАРию (USER)
            User maria = new User();
            maria.setName("Мария");
            maria.setEmail("maria@test.ru");
            // ШИФРУЕМ ПАРОЛЬ ПРЯМО ЗДЕСЬ ГАРАНТИРОВАННО
            maria.setPassword(passwordEncoder.encode("Secret123"));
            maria.setRole(User.Role.USER);
            userRepositoriy.save(maria);

            // 2. Создаем АНДРЕЯ (ADMIN)
            User admin = new User();
            admin.setName("Андрей");
            admin.setEmail("admin@test.ru");
            admin.setPassword(passwordEncoder.encode("Admin2026!"));
            admin.setRole(User.Role.ADMIN);
            userRepositoriy.save(admin);

            System.out.println(">>> Пользователи Мария и Андрей успешно созданы в базе!");
        } else {
            System.out.println(">>> Пользователи уже существуют.");
        }
    }
}