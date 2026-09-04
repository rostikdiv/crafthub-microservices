package com.milhub.user_service.controller;

import com.milhub.user_service.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserFileUploadController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "avatarsBucket", "avatars");
        ReflectionTestUtils.setField(controller, "documentsBucket", "documents");
    }

    @Test
    void uploadAvatar_ShouldCallService() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "img".getBytes());
        when(fileStorageService.uploadFile(file, "avatars")).thenReturn("http://minio/avatar.png");

        ResponseEntity<Map<String, String>> response = controller.uploadAvatar(file);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("url", "http://minio/avatar.png");
        verify(fileStorageService).uploadFile(file, "avatars");
    }

    @Test
    void uploadVerificationDocument_ShouldCallService() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf".getBytes());
        when(fileStorageService.uploadFile(file, "documents")).thenReturn("http://minio/doc.pdf");

        ResponseEntity<Map<String, String>> response = controller.uploadVerificationDocument(file);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("url", "http://minio/doc.pdf");
        verify(fileStorageService).uploadFile(file, "documents");
    }
}
