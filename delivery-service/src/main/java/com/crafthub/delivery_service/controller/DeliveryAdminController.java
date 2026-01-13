package com.crafthub.delivery_service.controller;

import com.crafthub.delivery_service.dto.request.BranchCreateDTO;
import com.crafthub.delivery_service.dto.request.BranchUpdateDTO;
import com.crafthub.delivery_service.dto.request.LocationCreateDTO;
import com.crafthub.delivery_service.dto.request.LocationUpdateDTO;
import com.crafthub.delivery_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/delivery") // Окремий шлях для адміна
@RequiredArgsConstructor
public class DeliveryAdminController {

    private final LocationService locationService;

    // POST: Завантажити список міст разом з відділеннями
    @PostMapping("/locations/import")
    public ResponseEntity<String> importLocations(@RequestBody List<LocationCreateDTO> request) {
        locationService.importLocations(request);
        return ResponseEntity.ok("Successfully imported " + request.size() + " locations with branches.");
    }
    @PostMapping("/locations/{locationId}/branches")
    public ResponseEntity<String> addBranch(
            @PathVariable UUID locationId,
            @RequestBody BranchCreateDTO dto
    ) {
        locationService.addBranchToLocation(locationId, dto);
        return ResponseEntity.ok("Branch added successfully");
    }

    // 2. Редагувати місто
    @PutMapping("/locations/{locationId}")
    public ResponseEntity<String> updateLocation(
            @PathVariable UUID locationId,
            @RequestBody LocationUpdateDTO dto
    ) {
        locationService.updateLocation(locationId, dto);
        return ResponseEntity.ok("Location updated");
    }

    // 3. Редагувати відділення
    @PutMapping("/branches/{branchId}")
    public ResponseEntity<String> updateBranch(
            @PathVariable UUID branchId,
            @RequestBody BranchUpdateDTO dto
    ) {
        locationService.updateBranch(branchId, dto);
        return ResponseEntity.ok("Branch updated");
    }

    // 4. Видалити місто (разом з відділеннями)
    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<String> deleteLocation(@PathVariable UUID locationId) {
        locationService.deleteLocation(locationId);
        return ResponseEntity.ok("Location and its branches deleted");
    }

    // 5. Видалити одне відділення
    @DeleteMapping("/branches/{branchId}")
    public ResponseEntity<String> deleteBranch(@PathVariable UUID branchId) {
        locationService.deleteBranch(branchId);
        return ResponseEntity.ok("Branch deleted");
    }
}