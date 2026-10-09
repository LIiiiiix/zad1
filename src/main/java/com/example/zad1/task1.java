package com.example.zad1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class task1 {

    @GetMapping("/message")
    public String getMessage() {
        return "Приветствую вас, дорогой пользователь! Вот данный ответ.";
    }

    @GetMapping("/number")
    public int getNumber() {
        return 42;
    }
}
