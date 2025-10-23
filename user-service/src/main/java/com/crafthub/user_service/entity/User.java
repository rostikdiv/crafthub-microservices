package com.crafthub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // Явно вказуємо назву таблиці
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL добре працює з IDENTITY
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash; // Ми *ніколи* не зберігаємо паролі у відкритому вигляді

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // (Ми додамо @Enumerated Role (роль) тут на Фазі 3)
}