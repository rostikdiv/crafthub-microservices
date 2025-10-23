package com.crafthub.user_service.controller;

import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor // Lombok: автоматично "інжектує" UserRepository через конструктор
public class UserController {

    private final UserRepository userRepository;

    // Це тимчасовий ендпоінт для перевірки, ми його потім захистимо
    @GetMapping(value = {"", "/"})
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}