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

        // 3. Ensure bucket exists if possible
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            // Bucket existence check may fail with 403 AccessDenied in GCS S3-interoperability mode
            // if permissions are restricted to object-level access. Proceed to upload.
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty() || "application/octet-stream".equals(contentType)) {
            contentType = determineContentTypeByExtension(extension);
        }

        // 4. Upload to MinIO / GCS
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
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
        if (url == null)
            return null;

        if (url.contains("/" + bucketName + "/")) {
            return url.substring(url.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
        }

        if (url.contains(bucketName)) {
            String afterBucket = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
            return afterBucket;
        }

        // Fallback: extract last segment
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }

        return url;
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
     * Determines MIME content-type based on file extension.
     */
    public static String determineContentTypeByExtension(String extension) {
        if (extension == null) return "application/octet-stream";
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
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