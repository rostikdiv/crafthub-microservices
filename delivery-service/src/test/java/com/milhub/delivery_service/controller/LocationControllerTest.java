package com.milhub.delivery_service.controller;

import com.milhub.delivery_service.dto.location.BranchResponseDTO;
import com.milhub.delivery_service.dto.location.LocationResponseDTO;
import com.milhub.delivery_service.dto.request.BranchUpdateDTO;
import com.milhub.delivery_service.dto.request.LocationCreateDTO;
import com.milhub.delivery_service.dto.request.LocationUpdateDTO;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationController controller;

    @Test
    void getAllLocations_ShouldReturnPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LocationResponseDTO> page = new PageImpl<>(List.of());
        when(locationService.getAllLocations(pageable)).thenReturn(page);

        ResponseEntity<Page<LocationResponseDTO>> response = controller.getAllLocations(pageable);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(locationService).getAllLocations(pageable);
    }

    @Test
    void getRegions_ShouldReturnListOfRegions() {
        when(locationService.getRegions(DeliveryProvider.NOVA_POSHTA)).thenReturn(List.of("Київська"));

        ResponseEntity<List<String>> response = controller.getRegions(DeliveryProvider.NOVA_POSHTA);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly("Київська");
        verify(locationService).getRegions(DeliveryProvider.NOVA_POSHTA);
    }

    @Test
    void searchCities_ShouldReturnMatchingLocations() {
        LocationResponseDTO dto = new LocationResponseDTO(UUID.randomUUID(), "ext-1", "Київ", "Київська");
        when(locationService.searchCities(DeliveryProvider.NOVA_POSHTA, "Київ", "Київська"))
                .thenReturn(List.of(dto));

        ResponseEntity<List<LocationResponseDTO>> response = controller.searchCities(
                DeliveryProvider.NOVA_POSHTA, "Київ", "Київська");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        verify(locationService).searchCities(DeliveryProvider.NOVA_POSHTA, "Київ", "Київська");
    }

    @Test
    void getBranches_ShouldReturnBranchesForCity() {
        UUID cityId = UUID.randomUUID();
        BranchResponseDTO dto = new BranchResponseDTO(UUID.randomUUID(), "ext-1", "1", "Відділення 1");
        when(locationService.getBranchesByCity(cityId)).thenReturn(List.of(dto));

        ResponseEntity<List<BranchResponseDTO>> response = controller.getBranches(cityId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        verify(locationService).getBranchesByCity(cityId);
    }

    @Test
    void importLocations_ShouldCallService() {
        LocationCreateDTO dto = new LocationCreateDTO(DeliveryProvider.NOVA_POSHTA, "ext-1", "Київ", "Київська", null);

        ResponseEntity<String> response = controller.importLocations(List.of(dto));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Imported 1 locations");
        verify(locationService).importLocations(List.of(dto));
    }

    @Test
    void updateLocation_ShouldCallService() {
        UUID locationId = UUID.randomUUID();
        LocationUpdateDTO dto = new LocationUpdateDTO("Київ", "Київська", DeliveryProvider.NOVA_POSHTA);

        ResponseEntity<Void> response = controller.updateLocation(locationId, dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(locationService).updateLocation(locationId, dto);
    }

    @Test
    void deleteLocation_ShouldCallService() {
        UUID locationId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteLocation(locationId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(locationService).deleteLocation(locationId);
    }

    @Test
    void getAllBranches_ShouldReturnPagedBranches() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<BranchResponseDTO> page = new PageImpl<>(List.of());
        when(locationService.getAllBranches(DeliveryProvider.NOVA_POSHTA, pageable)).thenReturn(page);

        ResponseEntity<Page<BranchResponseDTO>> response = controller.getAllBranches(DeliveryProvider.NOVA_POSHTA, pageable);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(locationService).getAllBranches(DeliveryProvider.NOVA_POSHTA, pageable);
    }

    @Test
    void updateBranch_ShouldCallService() {
        UUID branchId = UUID.randomUUID();
        BranchUpdateDTO dto = new BranchUpdateDTO("1", "Відділення 1");

        ResponseEntity<Void> response = controller.updateBranch(branchId, dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(locationService).updateBranch(branchId, dto);
    }

    @Test
    void deleteBranch_ShouldCallService() {
        UUID branchId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteBranch(branchId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(locationService).deleteBranch(branchId);
    }
}
