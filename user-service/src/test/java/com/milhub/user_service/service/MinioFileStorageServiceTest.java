package com.milhub.user_service.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
        ReflectionTestUtils.setField(storageService, "externalUrl", "http://localhost:9000");
    }

    @Test
    @DisplayName("uploadFile: throws RuntimeException when file is empty")
    void uploadFile_WhenFileEmpty_ShouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storageService.uploadFile(emptyFile, "verification-docs"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File must not be empty");

        verifyNoInteractions(minioClient);
    }

    @Test
    @DisplayName("uploadFile: uploads file and returns full url when bucket exists")
    void uploadFile_WhenBucketExists_ShouldUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String url = storageService.uploadFile(file, "verification-docs");

        assertThat(url).startsWith("http://localhost:9000/verification-docs/");
        assertThat(url).endsWith(".pdf");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadFile: creates bucket if bucket does not exist")
    void uploadFile_WhenBucketDoesNotExist_ShouldCreateBucket() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "png data".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String url = storageService.uploadFile(file, "avatars");

        assertThat(url).isNotNull();
        verify(minioClient).makeBucket(any());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadFile: resolves contentType from extension when contentType is null, empty or octet-stream")
    void uploadFile_WhenContentTypeMissing_ShouldDetermineFromExtension() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "doc.docx", null, "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "doc.jpg", "", "data".getBytes());
        MockMultipartFile file3 = new MockMultipartFile("file", "doc.webp", "application/octet-stream", "data".getBytes());
        MockMultipartFile file4 = new MockMultipartFile("file", "binaryfile", "application/octet-stream", "data".getBytes());

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.uploadFile(file1, "docs");
        storageService.uploadFile(file2, "docs");
        storageService.uploadFile(file3, "docs");
        storageService.uploadFile(file4, "docs");

        verify(minioClient, times(4)).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("getFile: returns input stream from MinioClient")
    void getFile_ShouldReturnStream() throws Exception {
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        InputStream result = storageService.getFile("docs", "file.pdf");

        assertThat(result).isNotNull();
        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    @DisplayName("extractObjectNameFromUrl: covers all URL variations and null")
    void extractObjectNameFromUrl_ShouldHandleAllFormats() {
        assertThat(storageService.extractObjectNameFromUrl(null, "docs")).isNull();
        assertThat(storageService.extractObjectNameFromUrl("http://localhost:9000/docs/order-123.pdf", "docs"))
                .isEqualTo("order-123.pdf");
        assertThat(storageService.extractObjectNameFromUrl("http://cdn.milhub.ua/docs-order-123.pdf", "docs"))
                .isEqualTo("order-123.pdf");
        assertThat(storageService.extractObjectNameFromUrl("http://otherhost/somepath/file.txt", "docs"))
                .isEqualTo("file.txt");
        assertThat(storageService.extractObjectNameFromUrl("plainfilename.txt", "docs"))
                .isEqualTo("plainfilename.txt");
    }

    @Test
    @DisplayName("determineContentTypeByExtension: covers all switch branches")
    void determineContentTypeByExtension_ShouldCoverAllBranches() {
        assertThat(FileStorageService.determineContentTypeByExtension(null)).isEqualTo("application/octet-stream");
        assertThat(FileStorageService.determineContentTypeByExtension("pdf")).isEqualTo("application/pdf");
        assertThat(FileStorageService.determineContentTypeByExtension("docx")).contains("wordprocessingml");
        assertThat(FileStorageService.determineContentTypeByExtension("doc")).isEqualTo("application/msword");
        assertThat(FileStorageService.determineContentTypeByExtension("png")).isEqualTo("image/png");
        assertThat(FileStorageService.determineContentTypeByExtension("jpg")).isEqualTo("image/jpeg");
        assertThat(FileStorageService.determineContentTypeByExtension("jpeg")).isEqualTo("image/jpeg");
        assertThat(FileStorageService.determineContentTypeByExtension("webp")).isEqualTo("image/webp");
        assertThat(FileStorageService.determineContentTypeByExtension("unknown")).isEqualTo("application/octet-stream");
    }
}
