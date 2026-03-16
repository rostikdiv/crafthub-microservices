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

    /**
     * Retrieves all locations. Useful for administrative views.
     *
     * @param pageable pagination and sorting information
     * @return a paginated list of locations
     */
    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<LocationResponseDTO>> getAllLocations(
            @org.springframework.data.web.PageableDefault(size = 20, sort = "nameUkr") org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(locationService.getAllLocations(pageable));
    }

    /**
     * Retrieves a list of regions for a specific delivery provider.
     *
     * @param provider the delivery provider (e.g., NOVA_POSHTA)
     * @return a list of region names
     */
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getRegions(@RequestParam DeliveryProvider provider) {
        return ResponseEntity.ok(locationService.getRegions(provider));
    }

    /**
     * Searches for cities based on name, provider, and optionally region.
     *
     * @param provider the delivery provider
     * @param query    search term for the city name
     * @param region   (optional) the region to filter by
     * @return a list of matching locations
     */
    @GetMapping("/cities")
    public ResponseEntity<List<LocationResponseDTO>> searchCities(
            @RequestParam DeliveryProvider provider,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(locationService.searchCities(provider, query, region));
    }

    /**
     * Retrieves branches for a specific city.
     *
     * @param cityId the unique identifier of the city/location
     * @return a list of branches in the specified city
     */
    @GetMapping("/branches")
    public ResponseEntity<List<BranchResponseDTO>> getBranches(
            @RequestParam UUID cityId) {
        return ResponseEntity.ok(locationService.getBranchesByCity(cityId));
    }

    /**
     * Imports location and branch data. Primary used for initial seeding/testing.
     *
     * @param locations list of location and branch data to import
     * @return a confirmation message
     */
    @PostMapping("/import")
    public ResponseEntity<String> importLocations(
            @RequestBody List<com.crafthub.delivery_service.dto.request.LocationCreateDTO> locations) {
        locationService.importLocations(locations);
        return ResponseEntity.ok("Imported " + locations.size() + " locations");
    }

    /**
     * Updates an existing location's details.
     *
     * @param id  the unique identifier of the location
     * @param dto updated location data
     * @return an empty ResponseEntity
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID id,
            @RequestBody com.crafthub.delivery_service.dto.request.LocationUpdateDTO dto) {
        locationService.updateLocation(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a location and its associated branches.
     *
     * @param id the unique identifier of the location
     * @return an empty ResponseEntity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Performs a global search for all branches of a specific provider.
     *
     * @param provider the delivery provider
     * @param pageable pagination information
     * @return a paginated list of branches
     */
    @GetMapping("/branches/all")
    public ResponseEntity<org.springframework.data.domain.Page<BranchResponseDTO>> getAllBranches(
            @RequestParam DeliveryProvider provider,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(locationService.getAllBranches(provider, pageable));
    }

    /**
     * Updates an existing branch's details.
     *
     * @param id  the unique identifier of the branch
     * @param dto updated branch data
     * @return an empty ResponseEntity
     */
    @PutMapping("/branches/{id}")
    public ResponseEntity<Void> updateBranch(
            @PathVariable UUID id,
            @RequestBody com.crafthub.delivery_service.dto.request.BranchUpdateDTO dto) {
        locationService.updateBranch(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a specific branch.
     *
     * @param id the unique identifier of the branch
     * @return an empty ResponseEntity
     */
    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        locationService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}