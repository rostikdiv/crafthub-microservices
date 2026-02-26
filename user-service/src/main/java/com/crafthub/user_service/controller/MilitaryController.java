package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.dto.UserResponseDTO;
import com.crafthub.user_service.service.ProfileService;
import com.crafthub.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/military")
@RequiredArgsConstructor
public class MilitaryController {

    private final ProfileService profileService;
    private final UserService userService;

    // Створити профіль військового
    @PostMapping("/profile")
    public ResponseEntity<String> createMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.createMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile created");
    }

    // Отримати поточний профіль
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getCurrentMilitaryProfile() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(userService.getUserByIdWithProfiles(userId));
    }

    // Додати документ верифікації
    @PostMapping("/documents")
    public ResponseEntity<String> addVerificationDocument(@RequestBody VerificationDocRequestDTO dto) {
        profileService.addVerificationDocument(dto);
        return ResponseEntity.ok("Document uploaded successfully");
    }
}
