package com.milhub.delivery_service.controller;

import com.milhub.delivery_service.dto.request.BranchCreateDTO;
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
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryAdminControllerTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private DeliveryAdminController controller;

    @Test
    void importLocations_ShouldCallService() {
        LocationCreateDTO dto = new LocationCreateDTO(DeliveryProvider.NOVA_POSHTA, "ext-1", "Київ", "Київська", null);

        ResponseEntity<String> response = controller.importLocations(List.of(dto));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Successfully imported locations");
        verify(locationService).importLocations(List.of(dto));
    }

    @Test
    void addBranch_ShouldCallService() {
        UUID locationId = UUID.randomUUID();
        BranchCreateDTO dto = new BranchCreateDTO("ext-br", "1", "Відділення 1");

        ResponseEntity<String> response = controller.addBranch(locationId, dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Branch added successfully");
        verify(locationService).addBranchToLocation(locationId, dto);
    }

    @Test
    void updateLocation_ShouldCallService() {
        UUID locationId = UUID.randomUUID();
        LocationUpdateDTO dto = new LocationUpdateDTO("Київ", "Київська", DeliveryProvider.NOVA_POSHTA);

        ResponseEntity<String> response = controller.updateLocation(locationId, dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Location updated");
        verify(locationService).updateLocation(locationId, dto);
    }

    @Test
    void updateBranch_ShouldCallService() {
        UUID branchId = UUID.randomUUID();
        BranchUpdateDTO dto = new BranchUpdateDTO("2", "Відділення 2");

        ResponseEntity<String> response = controller.updateBranch(branchId, dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Branch updated");
        verify(locationService).updateBranch(branchId, dto);
    }

    @Test
    void deleteLocation_ShouldCallService() {
        UUID locationId = UUID.randomUUID();

        ResponseEntity<String> response = controller.deleteLocation(locationId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Location deleted");
        verify(locationService).deleteLocation(locationId);
    }

    @Test
    void deleteBranch_ShouldCallService() {
        UUID branchId = UUID.randomUUID();

        ResponseEntity<String> response = controller.deleteBranch(branchId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Branch deleted");
        verify(locationService).deleteBranch(branchId);
    }
}
