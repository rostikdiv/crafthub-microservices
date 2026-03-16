package com.crafthub.delivery_service.controller;

import com.crafthub.delivery_service.dto.request.*;
import com.crafthub.delivery_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Administrative controller for managing delivery locations and branches.
 * Restricted to users with administrative privileges.
 */
@RestController
@RequestMapping("/api/v1/admin/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('order:read:all')") // Or a specific permission like location:manage
public class DeliveryAdminController {

    private final LocationService locationService;

    @PostMapping("/locations/import")
    public ResponseEntity<String> importLocations(@RequestBody List<LocationCreateDTO> request) {
        locationService.importLocations(request);
        return ResponseEntity.ok("Successfully imported locations.");
    }

    @PostMapping("/locations/{locationId}/branches")
    public ResponseEntity<String> addBranch(@PathVariable UUID locationId, @RequestBody BranchCreateDTO dto) {
        locationService.addBranchToLocation(locationId, dto);
        return ResponseEntity.ok("Branch added successfully");
    }

    @PutMapping("/locations/{locationId}")
    public ResponseEntity<String> updateLocation(@PathVariable UUID locationId, @RequestBody LocationUpdateDTO dto) {
        locationService.updateLocation(locationId, dto);
        return ResponseEntity.ok("Location updated");
    }

    @PutMapping("/branches/{branchId}")
    public ResponseEntity<String> updateBranch(@PathVariable UUID branchId, @RequestBody BranchUpdateDTO dto) {
        locationService.updateBranch(branchId, dto);
        return ResponseEntity.ok("Branch updated");
    }

    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<String> deleteLocation(@PathVariable UUID locationId) {
        locationService.deleteLocation(locationId);
        return ResponseEntity.ok("Location deleted");
    }

    @DeleteMapping("/branches/{branchId}")
    public ResponseEntity<String> deleteBranch(@PathVariable UUID branchId) {
        locationService.deleteBranch(branchId);
        return ResponseEntity.ok("Branch deleted");
    }
}