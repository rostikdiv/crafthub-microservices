package com.milhub.user_service.controller;

import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.service.SellerPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing pickup points dedicated to sellers.
 */
@RestController
@RequestMapping("/api/v1/sellers/points")
@RequiredArgsConstructor
public class SellerPointController {

    private final SellerPointService pointService;

    /**
     * Creates a new pickup point for the seller.
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerPointDTO> createPoint(@RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(pointService.createPoint(getCurrentUserId(), dto));
    }

    /**
     * Retrieves all pickup points belonging to the current seller.
     */
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<SellerPointDTO>> getMyPoints() {
        return ResponseEntity.ok(pointService.getMyPoints(getCurrentUserId()));
    }

    /**
     * Updates an existing pickup point's details.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerPointDTO> updatePoint(@PathVariable UUID id, @RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(pointService.updatePoint(getCurrentUserId(), id, dto));
    }

    /**
     * Deletes a pickup point.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deletePoint(@PathVariable UUID id) {
        pointService.deletePoint(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extracts the current user's ID from the security context.
     */
    private UUID getCurrentUserId() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(userIdStr);
    }
}
