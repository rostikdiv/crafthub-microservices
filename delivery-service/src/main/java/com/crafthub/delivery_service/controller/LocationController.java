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

    // КРОК 0: Отримати список областей
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
}