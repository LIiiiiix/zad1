package com.example.zad1.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "app_user")  // ← ДОБАВИТЬ ЭТУ СТРОКУ!
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}
