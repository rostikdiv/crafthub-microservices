package com.milhub.user_service.controller;

import com.milhub.user_service.dto.seller.SellerPublicProfileDTO;
import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.service.ProfileService;
import com.milhub.user_service.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for public-facing seller information and profile management.
 */
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final ProfileService profileService;

    /**
     * Retrieves a list of all seller public profiles (for filter dropdowns, etc.)
     */
    @GetMapping
    public ResponseEntity<java.util.List<SellerPublicProfileDTO>> getAllSellers() {
        return ResponseEntity.ok(sellerService.getAllSellers());
    }

    /**
     * Retrieves the public profile of a seller by their ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SellerPublicProfileDTO> getSellerProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(sellerService.getSellerPublicProfile(id));
    }

    /**
     * Creates a new seller profile for the current user.
     *
     * @param dto The seller profile details.
     * @return A response indicator.
     */
    @PostMapping("/profile")
    public ResponseEntity<String> createSellerProfile(@RequestBody SellerProfileRequestDTO dto) {
        profileService.createSellerProfile(dto);
        return ResponseEntity.ok("Seller profile created");
    }

    /**
     * Updates the existing seller profile for the current user.
     *
     * @param dto The updated profile details.
     * @return A response indicator.
     */
    @PutMapping("/profile")
    public ResponseEntity<String> updateSellerProfile(@RequestBody SellerProfileRequestDTO dto) {
        profileService.updateSellerProfile(dto);
        return ResponseEntity.ok("Seller profile updated");
    }

    /**
     * Internal endpoint used by other services to increment a seller's total sales
     * count.
     *
     * @param id The seller's ID.
     * @return A standard success response.
     */
    @PostMapping("/internal/{id}/sales/increment")
    public ResponseEntity<Void> incrementSales(@PathVariable UUID id) {
        sellerService.incrementSales(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Internal endpoint used by order-service to get seller auto-confirm config.
     */
    @GetMapping("/internal/{id}/auto-confirm")
    public ResponseEntity<Boolean> getAutoConfirm(@PathVariable UUID id) {
        return ResponseEntity.ok(sellerService.getAutoConfirm(id));
    }
}