package com.milhub.delivery_service.service;

import com.milhub.delivery_service.dto.location.BranchResponseDTO;
import com.milhub.delivery_service.dto.location.LocationResponseDTO;
import com.milhub.delivery_service.dto.request.BranchCreateDTO;
import com.milhub.delivery_service.dto.request.BranchUpdateDTO;
import com.milhub.delivery_service.dto.request.LocationCreateDTO;
import com.milhub.delivery_service.dto.request.LocationUpdateDTO;
import com.milhub.delivery_service.entity.Branch;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.Location;
import com.milhub.delivery_service.exception.ResourceNotFoundException;
import com.milhub.delivery_service.repository.BranchRepository;
import com.milhub.delivery_service.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private LocationService locationService;

    private Location testLocation;
    private Branch testBranch;
    private final UUID locationId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testLocation = Location.builder()
                .id(locationId)
                .provider(DeliveryProvider.NOVA_POSHTA)
                .externalId("ext-loc-1")
                .nameUkr("Київ")
                .region("Київська")
                .build();

        testBranch = Branch.builder()
                .id(branchId)
                .location(testLocation)
                .externalId("ext-br-1")
                .branchNumber("1")
                .name("Відділення №1")
                .build();
    }

    @Test
    void getRegions_ShouldReturnDistinctRegions() {
        when(locationRepository.findDistinctRegionsByProvider(DeliveryProvider.NOVA_POSHTA))
                .thenReturn(List.of("Київська", "Львівська"));

        List<String> regions = locationService.getRegions(DeliveryProvider.NOVA_POSHTA);

        assertThat(regions).containsExactly("Київська", "Львівська");
    }

    @Test
    void searchCities_WhenRegionIsProvided_ShouldQueryWithRegion() {
        when(locationRepository.findByProviderAndRegionAndNameUkrContainingIgnoreCase(
                DeliveryProvider.NOVA_POSHTA, "Київська", "Київ"))
                .thenReturn(List.of(testLocation));

        List<LocationResponseDTO> results = locationService.searchCities(
                DeliveryProvider.NOVA_POSHTA, "Київ", "Київська");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Київ");
        assertThat(results.get(0).region()).isEqualTo("Київська");
    }

    @Test
    void searchCities_WhenRegionIsNull_ShouldQueryWithoutRegion() {
        when(locationRepository.findByProviderAndNameUkrContainingIgnoreCase(
                DeliveryProvider.NOVA_POSHTA, "Київ"))
                .thenReturn(List.of(testLocation));

        List<LocationResponseDTO> results = locationService.searchCities(
                DeliveryProvider.NOVA_POSHTA, "Київ", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Київ");
    }

    @Test
    void searchCities_WhenRegionIsBlank_ShouldQueryWithoutRegion() {
        when(locationRepository.findByProviderAndNameUkrContainingIgnoreCase(
                DeliveryProvider.NOVA_POSHTA, "Київ"))
                .thenReturn(List.of(testLocation));

        List<LocationResponseDTO> results = locationService.searchCities(
                DeliveryProvider.NOVA_POSHTA, "Київ", "   ");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Київ");
    }

    @Test
    void getAllLocations_ShouldReturnPagedLocations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> page = new PageImpl<>(List.of(testLocation));
        when(locationRepository.findAll(pageable)).thenReturn(page);

        Page<LocationResponseDTO> result = locationService.getAllLocations(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(locationId);
    }

    @Test
    void getBranchesByCity_ShouldReturnBranchesForCity() {
        when(branchRepository.findByLocationId(locationId)).thenReturn(List.of(testBranch));

        List<BranchResponseDTO> result = locationService.getBranchesByCity(locationId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(branchId);
        assertThat(result.get(0).branchNumber()).isEqualTo("1");
        assertThat(result.get(0).name()).isEqualTo("Відділення №1");
    }

    @Test
    void importLocations_WithBranches_ShouldSaveLocationAndBranches() {
        BranchCreateDTO branchDto = new BranchCreateDTO("ext-br-2", "2", "Відділення №2");
        LocationCreateDTO locationDto = new LocationCreateDTO(
                DeliveryProvider.NOVA_POSHTA, "ext-loc-2", "Львів", "Львівська", List.of(branchDto));

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location loc = invocation.getArgument(0);
            loc.setId(UUID.randomUUID());
            return loc;
        });

        locationService.importLocations(List.of(locationDto));

        verify(locationRepository).save(any(Location.class));
        verify(branchRepository).saveAll(anyList());
    }

    @Test
    void importLocations_WithoutBranches_ShouldSaveLocationOnly() {
        LocationCreateDTO locationDto = new LocationCreateDTO(
                DeliveryProvider.NOVA_POSHTA, "ext-loc-3", "Одеса", "Одеська", null);

        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        locationService.importLocations(List.of(locationDto));

        verify(locationRepository).save(any(Location.class));
        verify(branchRepository, never()).saveAll(anyList());
    }

    @Test
    void addBranchToLocation_WhenLocationFound_ShouldSaveBranch() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        BranchCreateDTO dto = new BranchCreateDTO("ext-br-new", "10", "Відділення №10");

        locationService.addBranchToLocation(locationId, dto);

        ArgumentCaptor<Branch> branchCaptor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepository).save(branchCaptor.capture());

        Branch savedBranch = branchCaptor.getValue();
        assertThat(savedBranch.getBranchNumber()).isEqualTo("10");
        assertThat(savedBranch.getName()).isEqualTo("Відділення №10");
        assertThat(savedBranch.getLocation()).isEqualTo(testLocation);
    }

    @Test
    void addBranchToLocation_WhenLocationNotFound_ShouldThrowException() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        BranchCreateDTO dto = new BranchCreateDTO("ext-br-new", "10", "Відділення №10");

        assertThrows(ResourceNotFoundException.class,
                () -> locationService.addBranchToLocation(locationId, dto));

        verify(branchRepository, never()).save(any());
    }

    @Test
    void updateLocation_WhenFoundWithProvider_ShouldUpdateFields() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        LocationUpdateDTO dto = new LocationUpdateDTO("Київ Оновлений", "Київщина", DeliveryProvider.UKRPOSHTA);

        locationService.updateLocation(locationId, dto);

        assertThat(testLocation.getNameUkr()).isEqualTo("Київ Оновлений");
        assertThat(testLocation.getRegion()).isEqualTo("Київщина");
        assertThat(testLocation.getProvider()).isEqualTo(DeliveryProvider.UKRPOSHTA);
    }

    @Test
    void updateLocation_WhenFoundWithoutProvider_ShouldKeepExistingProvider() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        LocationUpdateDTO dto = new LocationUpdateDTO("Київ Оновлений", "Київщина", null);

        locationService.updateLocation(locationId, dto);

        assertThat(testLocation.getNameUkr()).isEqualTo("Київ Оновлений");
        assertThat(testLocation.getRegion()).isEqualTo("Київщина");
        assertThat(testLocation.getProvider()).isEqualTo(DeliveryProvider.NOVA_POSHTA);
    }

    @Test
    void updateLocation_WhenNotFound_ShouldThrowException() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        LocationUpdateDTO dto = new LocationUpdateDTO("Київ", "Київська", DeliveryProvider.NOVA_POSHTA);

        assertThrows(ResourceNotFoundException.class,
                () -> locationService.updateLocation(locationId, dto));
    }

    @Test
    void updateBranch_WhenFound_ShouldUpdateFields() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(testBranch));

        BranchUpdateDTO dto = new BranchUpdateDTO("1-A", "Оновлене відділення");

        locationService.updateBranch(branchId, dto);

        assertThat(testBranch.getBranchNumber()).isEqualTo("1-A");
        assertThat(testBranch.getName()).isEqualTo("Оновлене відділення");
    }

    @Test
    void updateBranch_WhenNotFound_ShouldThrowException() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        BranchUpdateDTO dto = new BranchUpdateDTO("1-A", "Оновлене відділення");

        assertThrows(ResourceNotFoundException.class,
                () -> locationService.updateBranch(branchId, dto));
    }

    @Test
    void deleteLocation_WhenExists_ShouldDelete() {
        when(locationRepository.existsById(locationId)).thenReturn(true);

        locationService.deleteLocation(locationId);

        verify(locationRepository).deleteById(locationId);
    }

    @Test
    void deleteLocation_WhenNotExists_ShouldThrowException() {
        when(locationRepository.existsById(locationId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> locationService.deleteLocation(locationId));

        verify(locationRepository, never()).deleteById(any());
    }

    @Test
    void deleteBranch_WhenExists_ShouldDelete() {
        when(branchRepository.existsById(branchId)).thenReturn(true);

        locationService.deleteBranch(branchId);

        verify(branchRepository).deleteById(branchId);
    }

    @Test
    void deleteBranch_WhenNotExists_ShouldThrowException() {
        when(branchRepository.existsById(branchId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> locationService.deleteBranch(branchId));

        verify(branchRepository, never()).deleteById(any());
    }

    @Test
    void getAllBranches_ShouldFormatCityAndBranchName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Branch> page = new PageImpl<>(List.of(testBranch));
        when(branchRepository.findByLocationProvider(DeliveryProvider.NOVA_POSHTA, pageable)).thenReturn(page);

        Page<BranchResponseDTO> result = locationService.getAllBranches(DeliveryProvider.NOVA_POSHTA, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Київ, Відділення №1");
    }
}
