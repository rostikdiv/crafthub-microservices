package com.milhub.user_service.controller;

import com.milhub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing user-specific profiles (Seller and Military).
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Submits a request to create a seller profile for the current user.
     */
    @PostMapping("/seller-profile")
    public ResponseEntity<String> createSellerProfile(@RequestBody SellerProfileRequestDTO dto) {
        profileService.createSellerProfile(dto);
        return ResponseEntity.ok("Seller profile created");
    }

    /**
     * Submits a request to create a military profile for the current user.
     */
    @PostMapping("/military-profile")
    public ResponseEntity<String> createMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.createMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile created");
    }

    /**
     * Submits a request to update an existing military profile.
     */
    @PutMapping("/military-profile")
    public ResponseEntity<String> updateMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.updateMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile updated successfully");
    }
}