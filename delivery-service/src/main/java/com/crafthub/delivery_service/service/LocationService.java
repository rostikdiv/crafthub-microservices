package com.crafthub.delivery_service.service;

import com.crafthub.delivery_service.dto.location.BranchResponseDTO;
import com.crafthub.delivery_service.dto.location.LocationResponseDTO;
import com.crafthub.delivery_service.dto.request.LocationCreateDTO;
import com.crafthub.delivery_service.entity.Branch;
import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.Location;
import com.crafthub.delivery_service.repository.BranchRepository;
import com.crafthub.delivery_service.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final BranchRepository branchRepository;

    // 1. Пошук населених пунктів для конкретного провайдера
    @Transactional(readOnly = true)
    public List<LocationResponseDTO> searchCities(DeliveryProvider provider, String query) {
        return locationRepository.findByProviderAndNameUkrContainingIgnoreCase(provider, query)
                .stream()
                .map(loc -> new LocationResponseDTO(
                        loc.getId(),
                        loc.getExternalId(),
                        loc.getNameUkr(),
                        loc.getRegion()
                ))
                .toList();
    }

    // 2. Отримання відділень для конкретного міста
    @Transactional(readOnly = true)
    public List<BranchResponseDTO> getBranchesByCity(UUID cityId) {
        return branchRepository.findByLocationId(cityId)
                .stream()
                .map(branch -> new BranchResponseDTO(
                        branch.getId(),
                        branch.getExternalId(),
                        branch.getBranchNumber(),
                        branch.getName()
                ))
                .toList();
    }

    // ... існуючі методи пошуку ...

    @Transactional
    public void importLocations(List<LocationCreateDTO> dtos) {
        for (var dto : dtos) {
            // 1. Створюємо місто
            Location location = Location.builder()
                    .provider(dto.provider())
                    .externalId(dto.externalId())
                    .nameUkr(dto.nameUkr())
                    .region(dto.region())
                    .build();

            // Зберігаємо місто, щоб отримати ID
            location = locationRepository.save(location);

            // 2. Створюємо відділення (якщо є)
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
                .orElseThrow(() -> new RuntimeException("Location not found"));

        Branch branch = Branch.builder()
                .location(location)
                .externalId(dto.externalId())
                .branchNumber(dto.branchNumber())
                .name(dto.name())
                .build();

        branchRepository.save(branch);
    }

    // --- АДМІН: Редагування ---
    @Transactional
    public void updateLocation(UUID locationId, com.crafthub.delivery_service.dto.request.LocationUpdateDTO dto) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found"));

        location.setNameUkr(dto.nameUkr());
        location.setRegion(dto.region());
        // provider та externalId зазвичай не змінюють, бо це зламає зв'язки
    }

    @Transactional
    public void updateBranch(UUID branchId, com.crafthub.delivery_service.dto.request.BranchUpdateDTO dto) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setBranchNumber(dto.branchNumber());
        branch.setName(dto.name());
    }

    // --- АДМІН: Видалення ---
    @Transactional
    public void deleteLocation(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new RuntimeException("Location not found");
        }
        locationRepository.deleteById(locationId); // Видалить і відділення завдяки cascade
    }

    @Transactional
    public void deleteBranch(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new RuntimeException("Branch not found");
        }
        branchRepository.deleteById(branchId);
    }
}