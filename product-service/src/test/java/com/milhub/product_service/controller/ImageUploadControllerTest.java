package com.milhub.product_service.controller;

import com.milhub.product_service.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ImageUploadController imageUploadController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageUploadController, "productsBucket", "products");
    }

    @Test
    @DisplayName("uploadProductImage: uploads file and returns url in map")
    void uploadProductImage_ShouldReturnUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "image bytes".getBytes());
        when(fileStorageService.uploadFile(eq(file), eq("products"))).thenReturn("http://storage/products/image.jpg");

        ResponseEntity<Map<String, String>> response = imageUploadController.uploadProductImage(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("url", "http://storage/products/image.jpg");
        verify(fileStorageService).uploadFile(file, "products");
    }
}
