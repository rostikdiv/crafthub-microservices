package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.SellerInfoDTO;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID; // ✅ Важливий імпорт

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userRepository.findById(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("user service works!");
    }

    @GetMapping("/{userId}/seller-info")
    public ResponseEntity<SellerInfoDTO> getSellerInfo(@PathVariable UUID userId) {
        // Логіку можна винести в сервіс, але для простоти показую тут
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getSellerProfile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a seller");
        }

        SellerProfile profile = user.getSellerProfile();

        return ResponseEntity.ok(new SellerInfoDTO(
                user.getId(),
                profile.getCompanyName(),
                profile.getLogoUrl(),
                user.getIsVerified()
        ));
    }
}