package com.milhub.user_service.controller;

import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.dto.seller.SellerPublicProfileDTO;
import com.milhub.user_service.service.ProfileService;
import com.milhub.user_service.service.SellerService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private SellerController controller;

    @Test
    void getAllSellers_ShouldReturnList() {
        when(sellerService.getAllSellers()).thenReturn(List.of());

        ResponseEntity<List<SellerPublicProfileDTO>> response = controller.getAllSellers();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(sellerService).getAllSellers();
    }

    @Test
    void getSellerProfile_ShouldReturnProfile() {
        UUID sellerId = UUID.randomUUID();
        SellerPublicProfileDTO dto = new SellerPublicProfileDTO(
                sellerId, "MilCorp", "desc", "logo", 5.0f, 10, true, java.time.LocalDateTime.now(), List.of()
        );
        when(sellerService.getSellerPublicProfile(sellerId)).thenReturn(dto);

        ResponseEntity<SellerPublicProfileDTO> response = controller.getSellerProfile(sellerId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
        verify(sellerService).getSellerPublicProfile(sellerId);
    }

    @Test
    void createSellerProfile_ShouldCallProfileService() {
        SellerProfileRequestDTO dto = new SellerProfileRequestDTO(
                "MilCorp", "desc", "logo", "Tax1", false
        );

        ResponseEntity<String> response = controller.createSellerProfile(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Seller profile created");
        verify(profileService).createSellerProfile(dto);
    }

    @Test
    void updateSellerProfile_ShouldCallProfileService() {
        SellerProfileRequestDTO dto = new SellerProfileRequestDTO(
                "MilCorp", "desc", "logo", "Tax1", true
        );

        ResponseEntity<String> response = controller.updateSellerProfile(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Seller profile updated");
        verify(profileService).updateSellerProfile(dto);
    }

    @Test
    void incrementSales_ShouldCallSellerService() {
        UUID sellerId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.incrementSales(sellerId, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(sellerService).incrementSales(sellerId);
    }

    @Test
    void getAutoConfirm_ShouldCallSellerService() {
        UUID sellerId = UUID.randomUUID();
        when(sellerService.getAutoConfirm(sellerId)).thenReturn(true);

        ResponseEntity<Boolean> response = controller.getAutoConfirm(sellerId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isTrue();
        verify(sellerService).getAutoConfirm(sellerId);
    }
}
