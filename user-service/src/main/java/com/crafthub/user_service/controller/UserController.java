package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.SellerInfoDTO;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.exception.ResourceNotFoundException; // ✅
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Отримати всіх користувачів (Тільки для Адміна)
    @GetMapping
    @PreAuthorize("hasAuthority('user:ban')") // або ROLE_ADMIN
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id)));
    }

    // Видалення (Тільки для Адміна)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:ban')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Цей метод викликає Product Service через Feign.
    // Він має бути доступним для авторизованих сервісів (токен передається).
    @GetMapping("/{userId}/seller-info")
    public ResponseEntity<SellerInfoDTO> getSellerInfo(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getSellerProfile() == null) {
            // Це не критична помилка, просто у юзера немає профілю
            throw new ResourceNotFoundException("User " + userId + " is not a seller");
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