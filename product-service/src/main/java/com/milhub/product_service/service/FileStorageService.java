package com.milhub.product_service.service;

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
 * Service for managing file storage using MinIO.
 * Handles file uploads, bucket creation, and presigned URL generation.
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
     * Uploads a file to the specified MinIO bucket.
     * Generates a unique filename using a UUID.
     *
     * @param file       the file to upload
     * @param bucketName the destination bucket name
     * @return the full URL of the uploaded file
     */
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        if (file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        // 1. Generate a unique name
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + extension;

        // 2. Get input stream
        InputStream inputStream = file.getInputStream();

        // 3. Check and create bucket if it doesn't exist
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

        // 5. Return the URL
        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    /**
     * Generates a presigned URL for a specific object in a bucket.
     *
     * @param objectName name of the object
     * @param bucketName name of the bucket
     * @return presigned URL valid for 24 hours
     */
    @SneakyThrows
    public String getPresignedUrl(String objectName, String bucketName) {
        MinioClient signingClient = MinioClient.builder()
                .endpoint(externalUrl)
                .credentials(accessKey, secretKey)
                .build();

        return signingClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(24 * 60 * 60) // 24 hours
                        .build());
    }

    /**
     * Extracts the object name from a full MinIO URL.
     *
     * @param url        the full URL
     * @param bucketName name of the bucket
     * @return the object name or null if not found
     */
    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null || !url.contains(bucketName))
            return null;
        String afterBucket = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
        return afterBucket;
    }

    /**
     * Helper method to get file extension.
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}