package com.milhub.user_service.controller;

import com.milhub.user_service.dto.auth.ChangePasswordRequestDTO;
import com.milhub.user_service.dto.seller.SellerInfoDTO;
import com.milhub.user_service.dto.user.UserUpdateDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for general user management and profile operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Retrieves all users in the system.
     * Restricted to administrators.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:ban')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAllWithProfiles().stream()
                        .map(userService::mapToResponseDTO)
                        .toList());
    }

    /**
     * Retrieves a specific user by their ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserByIdWithProfiles(id));
    }

    /**
     * Deletes a user from the system.
     * Restricted to administrators.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:ban')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves public seller information for a specific user.
     * Often used via Feign client from other services.
     */
    @GetMapping("/{userId}/seller-info")
    public ResponseEntity<SellerInfoDTO> getSellerInfo(@PathVariable UUID userId) {
        User user = userRepository.findByIdWithProfiles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getSellerProfile() == null) {
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
                profile.getTotalSales(),
                profile.getAutoConfirmOrders()));
    }

    /**
     * Updates the currently authenticated user's basic information.
     */
    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(@RequestBody @Valid UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateCurrentUser(dto));
    }

    /**
     * Changes the currently authenticated user's password.
     */
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequestDTO dto) {
        userService.changePassword(dto);
        return ResponseEntity.ok().build();
    }
}