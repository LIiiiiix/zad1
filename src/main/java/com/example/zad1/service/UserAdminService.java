package com.example.zad1.service;

import com.example.zad1.model.User;
import com.example.zad1.repositoriy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

// Управление пользователями - только для администратора
@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<User> getAll() {
        return userRepo.findAll();
    }

    public User changeStatus(Long id, User.Status status, String currentEmail) {
        User user = find(id);
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalStateException("Администратор не может изменить собственный статус");
        }
        if (status == null) {
            throw new IllegalArgumentException("status обязателен: ACTIVE или BLOCKED");
        }
        user.setStatus(status);
        return user;
    }

    public void delete(Long id, String currentEmail) {
        User user = find(id);
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalStateException("Администратор не может удалить сам себя");
        }
        userRepo.delete(user);
    }

    private User find(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Пользователь " + id + " не найден"));
    }
}