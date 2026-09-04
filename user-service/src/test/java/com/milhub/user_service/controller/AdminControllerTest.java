package com.milhub.user_service.controller;

import com.milhub.user_service.dto.admin.VerificationRequestResponseDTO;
import com.milhub.user_service.dto.admin.VerificationResponseDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.service.AdminService;
import com.milhub.user_service.service.UserService;
import com.milhub.user_service.service.VerificationDocService;
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
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private VerificationDocService docService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController controller;

    @Test
    void getPendingVerifications_ShouldCallAdminService() {
        when(adminService.getPendingVerifications()).thenReturn(List.of());

        ResponseEntity<List<VerificationRequestResponseDTO>> response = controller.getPendingVerifications();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(adminService).getPendingVerifications();
    }

    @Test
    void verifyUser_ShouldCallAdminService() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<String> response = controller.verifyUser(userId, true, "All good");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("User verification status updated");
        verify(adminService).verifyUser(userId, true, "All good");
    }

    @Test
    void verifyDoc_ShouldCallDocService() {
        UUID docId = UUID.randomUUID();

        ResponseEntity<String> response = controller.verifyDoc(docId, true);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("Document status updated");
        verify(docService).updateDocumentStatus(docId, true);
    }

    @Test
    void getUserDocuments_ShouldCallDocService() {
        UUID userId = UUID.randomUUID();
        when(docService.getDocumentsByUserId(userId)).thenReturn(List.of());

        ResponseEntity<List<VerificationResponseDTO>> response = controller.getUserDocuments(userId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(docService).getDocumentsByUserId(userId);
    }

    @Test
    void promoteToAdmin_ShouldCallUserService() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        UserResponseDTO dto = UserResponseDTO.builder().id(userId).build();

        when(userService.promoteUserToAdmin(userId)).thenReturn(user);
        when(userService.mapToResponseDTO(user)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.promoteToAdmin(userId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userService).promoteUserToAdmin(userId);
    }
}
