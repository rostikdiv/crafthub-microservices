package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.crafthub.user_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/military")
@RequiredArgsConstructor
public class MilitaryController {

    private final ProfileService profileService;

    // Створити профіль військового
    @PostMapping("/profile")
    public ResponseEntity<String> createMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.createMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile created");
    }
}
