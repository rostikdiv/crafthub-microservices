package com.milhub.user_service.controller;

import com.milhub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController controller;

    @Test
    void createSellerProfile_ShouldCallService() {
        SellerProfileRequestDTO dto = new SellerProfileRequestDTO(
                "Company", "desc", "logo.png", "Tax123", false
        );

        ResponseEntity<String> response = controller.createSellerProfile(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Seller profile created");
        verify(profileService).createSellerProfile(dto);
    }

    @Test
    void createMilitaryProfile_ShouldCallService() {
        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO("A1234", "12345678", "Officer", "Kyiv");

        ResponseEntity<String> response = controller.createMilitaryProfile(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Military profile created");
        verify(profileService).createMilitaryProfile(dto);
    }

    @Test
    void updateMilitaryProfile_ShouldCallService() {
        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO("A1234", "12345678", "Captain", "Kyiv");

        ResponseEntity<String> response = controller.updateMilitaryProfile(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Military profile updated successfully");
        verify(profileService).updateMilitaryProfile(dto);
    }
}
