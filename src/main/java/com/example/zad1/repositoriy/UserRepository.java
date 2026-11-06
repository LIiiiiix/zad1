package com.example.zad1.repositoriy;

import com.example.zad1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по email.
     * Используется сервисом аутентификации (AuthService) при попытке входа.
     */
    Optional<User> findByEmail(String email);
}