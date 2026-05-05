package com.crafthub.delivery_service.service;

import com.crafthub.delivery_service.dto.location.BranchResponseDTO;
import com.crafthub.delivery_service.dto.location.LocationResponseDTO;
import com.crafthub.delivery_service.dto.request.LocationCreateDTO;
import com.crafthub.delivery_service.entity.Branch;
import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.Location;
import com.crafthub.delivery_service.exception.ResourceNotFoundException; // ✅
import com.crafthub.delivery_service.repository.BranchRepository;
import com.crafthub.delivery_service.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing geographic locations (cities/regions) and delivery
 * service branches.
 * Supports importing, searching, and CRUD operations for delivery points.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<String> getRegions(DeliveryProvider provider) {
        return locationRepository.findDistinctRegionsByProvider(provider);
    }

    @Transactional(readOnly = true)
    public List<LocationResponseDTO> searchCities(DeliveryProvider provider, String query, String region) {
        List<Location> locations;
        if (region != null && !region.isBlank()) {
            locations = locationRepository.findByProviderAndRegionAndNameUkrContainingIgnoreCase(provider, region,
                    query);
        } else {
            locations = locationRepository.findByProviderAndNameUkrContainingIgnoreCase(provider, query);
        }

        return locations.stream()
                .map(loc -> new LocationResponseDTO(
                        loc.getId(), loc.getExternalId(), loc.getNameUkr(), loc.getRegion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<LocationResponseDTO> getAllLocations(
            org.springframework.data.domain.Pageable pageable) {
        return locationRepository.findAll(pageable)
                .map(loc -> new LocationResponseDTO(
                        loc.getId(), loc.getExternalId(), loc.getNameUkr(), loc.getRegion()));
    }

    @Transactional(readOnly = true)
    public List<BranchResponseDTO> getBranchesByCity(UUID cityId) {
        return branchRepository.findByLocationId(cityId)
                .stream()
                .map(branch -> new BranchResponseDTO(
                        branch.getId(), branch.getExternalId(), branch.getBranchNumber(), branch.getName()))
                .toList();
    }

    /**
     * Imports a list of locations and their associated branches into the database.
     *
     * @param dtos list of LocationCreateDTO objects containing data to import
     */
    @Transactional
    public void importLocations(List<LocationCreateDTO> dtos) {
        for (var dto : dtos) {
            Location location = Location.builder()
                    .provider(dto.provider())
                    .externalId(dto.externalId())
                    .nameUkr(dto.nameUkr())
                    .region(dto.region())
                    .build();
            location = locationRepository.save(location);

            if (dto.branches() != null && !dto.branches().isEmpty()) {
                Location finalLocation = location;
                List<Branch> branches = dto.branches().stream()
                        .map(bDto -> Branch.builder()
                                .location(finalLocation)
                                .externalId(bDto.externalId())
                                .branchNumber(bDto.branchNumber())
                                .name(bDto.name())
                                .build())
                        .toList();
                branchRepository.saveAll(branches);
            }
        }
    }

    @Transactional
    public void addBranchToLocation(UUID locationId, com.crafthub.delivery_service.dto.request.BranchCreateDTO dto) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found")); // ✅

        Branch branch = Branch.builder()
                .location(location)
                .externalId(dto.externalId())
                .branchNumber(dto.branchNumber())
                .name(dto.name())
                .build();
        branchRepository.save(branch);
    }

    @Transactional
    public void updateLocation(UUID locationId, com.crafthub.delivery_service.dto.request.LocationUpdateDTO dto) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found")); // ✅
        location.setNameUkr(dto.nameUkr());
        location.setRegion(dto.region());
        if (dto.provider() != null) {
            location.setProvider(dto.provider());
        }
    }

    @Transactional
    public void updateBranch(UUID branchId, com.crafthub.delivery_service.dto.request.BranchUpdateDTO dto) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found")); // ✅
        branch.setBranchNumber(dto.branchNumber());
        branch.setName(dto.name());
    }

    @Transactional
    public void deleteLocation(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found"); // ✅
        }
        locationRepository.deleteById(locationId);
    }

    @Transactional
    public void deleteBranch(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found"); // ✅
        }
        branchRepository.deleteById(branchId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BranchResponseDTO> getAllBranches(
            DeliveryProvider provider,
            org.springframework.data.domain.Pageable pageable) {

        return branchRepository.findByLocationProvider(provider, pageable)
                .map(branch -> new BranchResponseDTO(
                        branch.getId(),
                        branch.getExternalId(),
                        branch.getBranchNumber(),
                        // Combine City + Branch Name for full context
                        branch.getLocation().getNameUkr() + ", " + branch.getName()));
    }
}