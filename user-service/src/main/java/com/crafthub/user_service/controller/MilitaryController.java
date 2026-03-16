package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.dto.user.UserResponseDTO;
import com.crafthub.user_service.service.ProfileService;
import com.crafthub.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Controller for managing military-specific user profiles and verification
 * documents.
 */
@RestController
@RequestMapping("/api/v1/military")
@RequiredArgsConstructor
public class MilitaryController {

    private final ProfileService profileService;
    private final UserService userService;

    /**
     * Creates a new military profile for the currently authenticated user.
     */
    @PostMapping("/profile")
    public ResponseEntity<String> createMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.createMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile created");
    }

    /**
     * Retrieves the military profile details for the current user.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getCurrentMilitaryProfile() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(userService.getUserByIdWithProfiles(userId));
    }

    /**
     * Uploads and links a verification document to the user's profile.
     */
    @PostMapping("/documents")
    public ResponseEntity<String> addVerificationDocument(@RequestBody VerificationDocRequestDTO dto) {
        profileService.addVerificationDocument(dto);
        return ResponseEntity.ok("Document uploaded successfully");
    }
}
