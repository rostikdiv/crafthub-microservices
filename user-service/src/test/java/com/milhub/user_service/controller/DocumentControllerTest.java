package com.milhub.user_service.controller;

import com.milhub.user_service.service.VerificationDocService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private VerificationDocService docService;

    @InjectMocks
    private DocumentController controller;

    @Test
    void getDocument_WhenValidContentType_ShouldReturnResource() {
        UUID docId = UUID.randomUUID();
        ByteArrayInputStream is = new ByteArrayInputStream("dummy data".getBytes(StandardCharsets.UTF_8));
        VerificationDocService.DocumentDownloadDTO download =
                new VerificationDocService.DocumentDownloadDTO(is, "application/pdf", "doc.pdf");

        when(docService.downloadDocument(docId)).thenReturn(download);

        ResponseEntity<Resource> response = controller.getDocument(docId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("doc.pdf");
        verify(docService).downloadDocument(docId);
    }

    @Test
    void getDocument_WhenInvalidContentType_ShouldFallbackToOctetStream() {
        UUID docId = UUID.randomUUID();
        ByteArrayInputStream is = new ByteArrayInputStream("dummy data".getBytes(StandardCharsets.UTF_8));
        VerificationDocService.DocumentDownloadDTO download =
                new VerificationDocService.DocumentDownloadDTO(is, "invalid/type///", "doc.bin");

        when(docService.downloadDocument(docId)).thenReturn(download);

        ResponseEntity<Resource> response = controller.getDocument(docId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}
