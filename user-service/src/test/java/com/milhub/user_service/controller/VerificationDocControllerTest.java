package com.milhub.user_service.controller;

import com.milhub.user_service.dto.admin.VerificationResponseDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.service.VerificationDocService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationDocControllerTest {

    @Mock
    private VerificationDocService docService;

    @InjectMocks
    private VerificationDocController controller;

    @Test
    void uploadDoc_ShouldCallService() {
        VerificationDocRequestDTO request = new VerificationDocRequestDTO(DocumentType.MILITARY_ID, "http://doc.pdf");
        VerificationResponseDTO responseDTO = new VerificationResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), DocumentType.MILITARY_ID, "http://doc.pdf",
                VerificationStatus.PENDING, LocalDateTime.now()
        );

        when(docService.uploadDocument(request)).thenReturn(responseDTO);

        ResponseEntity<VerificationResponseDTO> response = controller.uploadDoc(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(docService).uploadDocument(request);
    }

    @Test
    void getMyDocs_ShouldCallService() {
        when(docService.getMyDocuments()).thenReturn(List.of());

        ResponseEntity<List<VerificationResponseDTO>> response = controller.getMyDocs();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(docService).getMyDocuments();
    }

    @Test
    void deleteDoc_ShouldCallService() {
        UUID docId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteDoc(docId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(docService).deleteDocument(docId);
    }
}
