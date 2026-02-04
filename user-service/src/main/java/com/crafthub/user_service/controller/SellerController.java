package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.SellerPublicProfileDTO;
import com.crafthub.user_service.dto.profile.SellerProfileRequestDTO;
import com.crafthub.user_service.service.ProfileService;
import com.crafthub.user_service.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final ProfileService profileService;

    // Публічний ендпоінт: Отримати сторінку продавця
    // GET /api/v1/sellers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SellerPublicProfileDTO> getSellerProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(sellerService.getSellerPublicProfile(id));
    }

    @PutMapping("/seller-profile")
    public ResponseEntity<String> updateSellerProfile(@RequestBody SellerProfileRequestDTO dto) {
        profileService.updateSellerProfile(dto);
        return ResponseEntity.ok("Seller profile updated");
    }
}