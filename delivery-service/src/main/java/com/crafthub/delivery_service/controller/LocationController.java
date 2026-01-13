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

    // КРОК 1: Отримати міста (пошук по назві + провайдер)
    // GET /api/v1/delivery/locations/cities?provider=NOVA_POSHTA&query=Льві
    @GetMapping("/cities")
    public ResponseEntity<List<LocationResponseDTO>> searchCities(
            @RequestParam DeliveryProvider provider,
            @RequestParam(required = false, defaultValue = "") String query
    ) {
        return ResponseEntity.ok(locationService.searchCities(provider, query));
    }

    // КРОК 2: Отримати відділення (по ID міста)
    // GET /api/v1/delivery/locations/branches?cityId=...
    @GetMapping("/branches")
    public ResponseEntity<List<BranchResponseDTO>> getBranches(
            @RequestParam UUID cityId
    ) {
        return ResponseEntity.ok(locationService.getBranchesByCity(cityId));
    }
}