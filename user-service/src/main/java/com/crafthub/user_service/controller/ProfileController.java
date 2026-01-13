package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.crafthub.user_service.dto.profile.SellerProfileRequestDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/seller-profile")
    public ResponseEntity<String> createSellerProfile(@RequestBody SellerProfileRequestDTO dto) {
        profileService.createSellerProfile(dto);
        return ResponseEntity.ok("Seller profile created");
    }

    @PostMapping("/military-profile")
    public ResponseEntity<String> createMilitaryProfile(@RequestBody MilitaryProfileRequestDTO dto) {
        profileService.createMilitaryProfile(dto);
        return ResponseEntity.ok("Military profile created");
    }

    @PostMapping("/verification-docs")
    public ResponseEntity<String> uploadDoc(@RequestBody VerificationDocRequestDTO dto) {
        profileService.uploadVerificationDoc(dto);
        return ResponseEntity.ok("Document uploaded for verification");
    }

    @GetMapping("/verification-docs")
    public ResponseEntity<List<VerificationDoc>> getMyDocs() {
        return ResponseEntity.ok(profileService.getMyDocuments());
    }
}