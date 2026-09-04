package com.milhub.product_service.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioFileStorageService storageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storageService, "minioUrl", "http://localhost:9000");
    }

    @Test
    @DisplayName("uploadFile: throws RuntimeException when file is empty")
    void uploadFile_WhenFileEmpty_ShouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadFile(emptyFile, "products"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File must not be empty");

        verifyNoInteractions(minioClient);
    }

    @Test
    @DisplayName("uploadFile: uploads file and returns full url")
    void uploadFile_WhenFileValid_ShouldUploadAndReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "tactical.png", "image/png", "test content".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String url = storageService.uploadFile(file, "products");

        assertThat(url).startsWith("http://localhost:9000/products/");
        assertThat(url).endsWith(".png");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadFile: creates bucket if it does not exist")
    void uploadFile_WhenBucketDoesNotExist_ShouldCreateBucket() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "tactical.png", "image/png", "test content".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String url = storageService.uploadFile(file, "products");

        assertThat(url).isNotNull();
        verify(minioClient).makeBucket(any());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("extractObjectNameFromUrl: extracts object name correctly")
    void extractObjectNameFromUrl_ShouldExtractCorrectName() {
        String url = "http://localhost:9000/products/image-123.jpg";
        String objectName = storageService.extractObjectNameFromUrl(url, "products");

        assertThat(objectName).isEqualTo("image-123.jpg");
    }

    @Test
    @DisplayName("extractObjectNameFromUrl: returns null when url is null or doesn't contain bucket")
    void extractObjectNameFromUrl_WhenNullOrNoBucket_ShouldReturnNull() {
        assertThat(storageService.extractObjectNameFromUrl(null, "products")).isNull();
        assertThat(storageService.extractObjectNameFromUrl("http://localhost:9000/other/1.jpg", "products")).isNull();
    }
}
