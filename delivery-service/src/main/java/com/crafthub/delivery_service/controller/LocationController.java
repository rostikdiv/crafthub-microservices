package com.crafthub.delivery_service.controller;

import com.crafthub.delivery_service.dto.location.BranchResponseDTO;
import com.crafthub.delivery_service.dto.location.LocationResponseDTO;
import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // КРОК 0: Отримати всі локації (для адмінки)
    // GET /api/v1/delivery/locations
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<LocationResponseDTO>> getAllLocations(
            @org.springframework.data.web.PageableDefault(size = 20, sort = "nameUkr") org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(locationService.getAllLocations(pageable));
    }

    // КРОК 1: Отримати список областей
    // GET /api/v1/delivery/locations/regions?provider=NOVA_POSHTA
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getRegions(@RequestParam DeliveryProvider provider) {
        return ResponseEntity.ok(locationService.getRegions(provider));
    }

    // КРОК 1: Отримати міста (пошук по назві + провайдер + область)
    // GET
    // /api/v1/delivery/locations/cities?provider=NOVA_POSHTA&query=Льві&region=Львівська
    // область
    @GetMapping("/cities")
    public ResponseEntity<List<LocationResponseDTO>> searchCities(
            @RequestParam DeliveryProvider provider,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(locationService.searchCities(provider, query, region));
    }

    // КРОК 2: Отримати відділення (по ID міста)
    // GET /api/v1/delivery/locations/branches?cityId=...
    @GetMapping("/branches")
    public ResponseEntity<List<BranchResponseDTO>> getBranches(
            @RequestParam UUID cityId) {
        return ResponseEntity.ok(locationService.getBranchesByCity(cityId));
    }

    // КРОК 3: Імпорт даних (для тестування)
    // POST /api/v1/delivery/locations/import
    @PostMapping("/import")
    public ResponseEntity<String> importLocations(
            @RequestBody List<com.crafthub.delivery_service.dto.request.LocationCreateDTO> locations) {
        locationService.importLocations(locations);
        return ResponseEntity.ok("Imported " + locations.size() + " locations");
    }

    // КРОК 4: Оновити локацію
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID id,
            @RequestBody com.crafthub.delivery_service.dto.request.LocationUpdateDTO dto) {
        locationService.updateLocation(id, dto);
        return ResponseEntity.ok().build();
    }

    // КРОК 5: Видалити локацію
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    // КРОК 6: Отримати всі відділення (Global Search)
    // GET /api/v1/delivery/locations/branches/all?provider=NOVA_POSHTA
    @GetMapping("/branches/all")
    public ResponseEntity<org.springframework.data.domain.Page<BranchResponseDTO>> getAllBranches(
            @RequestParam DeliveryProvider provider,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(locationService.getAllBranches(provider, pageable));
    }

    // КРОК 7: Оновити відділення
    @PutMapping("/branches/{id}")
    public ResponseEntity<Void> updateBranch(
            @PathVariable UUID id,
            @RequestBody com.crafthub.delivery_service.dto.request.BranchUpdateDTO dto) {
        locationService.updateBranch(id, dto);
        return ResponseEntity.ok().build();
    }

    // КРОК 8: Видалити відділення
    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        locationService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}