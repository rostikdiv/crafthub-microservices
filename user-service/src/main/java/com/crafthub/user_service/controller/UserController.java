package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.ChangePasswordRequestDTO;
import com.crafthub.user_service.dto.SellerInfoDTO;
import com.crafthub.user_service.dto.UserUpdateDTO;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.UserRepository;
import com.crafthub.user_service.service.UserService;
import jakarta.validation.Valid;
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
    private final UserService userService;

    // Отримати всіх користувачів (Тільки для Адміна)
    @GetMapping
    @PreAuthorize("hasAuthority('user:ban')") // або ROLE_ADMIN
    public ResponseEntity<List<com.crafthub.user_service.dto.UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(user -> userService.getUserByIdWithProfiles(user.getId()))
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.crafthub.user_service.dto.UserResponseDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserByIdWithProfiles(id));
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
                user.getIsVerified(),
                profile.getRating(),
                profile.getReviewCount(),
                profile.getTotalSales()));
    }

    // --- User Profile Updates ---

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(@RequestBody @Valid UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateCurrentUser(dto));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequestDTO dto) {
        userService.changePassword(dto);
        return ResponseEntity.ok().build();
    }
}