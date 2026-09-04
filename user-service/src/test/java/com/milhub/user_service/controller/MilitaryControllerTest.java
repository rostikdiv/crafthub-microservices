package com.milhub.user_service.controller;

import com.milhub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.service.ProfileService;
import com.milhub.user_service.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilitaryControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MilitaryController controller;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
    void getCurrentMilitaryProfile_ShouldCallUserService() {
        UserResponseDTO dto = UserResponseDTO.builder().id(userId).build();
        when(userService.getUserByIdWithProfiles(userId)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.getCurrentMilitaryProfile();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userService).getUserByIdWithProfiles(userId);
    }

    @Test
    void addVerificationDocument_ShouldCallProfileService() {
        VerificationDocRequestDTO dto = new VerificationDocRequestDTO(DocumentType.MILITARY_ID, "http://doc.pdf");

        ResponseEntity<String> response = controller.addVerificationDocument(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Document uploaded successfully");
        verify(profileService).addVerificationDocument(dto);
    }
}
