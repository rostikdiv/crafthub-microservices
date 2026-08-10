package com.milhub.user_service.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Service for handling file storage operations using MinIO (S3-compatible).
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.external-url:http://localhost:9000}")
    private String externalUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Uploads a file to a specified bucket. Generates a unique filename using UUID.
     */
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        if (file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        // 1. Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + extension;

        // 2. Get input stream
        InputStream inputStream = file.getInputStream();

        // 3. Ensure bucket exists
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 4. Upload to MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        // 5. Return internal URL
        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    /**
     * Generates a presigned URL for secure document access.
     */
    @SneakyThrows
    public String getPresignedUrl(String objectName, String bucketName) {
        return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(24 * 60 * 60) // 24 hours
                        .build());
    }

    /**
     * Helper to extract the object name (filename) from a storage URL.
     */
    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null || !url.contains(bucketName))
            return null;

        String afterBucket = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
        return afterBucket;
    }

    /**
     * Retrieves an input stream for a specific object in the storage.
     */
    @SneakyThrows
    public InputStream getFile(String bucketName, String objectName) {
        return minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    /**
     * Extracts the file extension from a filename.
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}