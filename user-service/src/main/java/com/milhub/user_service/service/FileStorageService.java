package com.milhub.user_service.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * Common interface for file storage operations across profiles (MinIO locally, GCS in cloud).
 */
public interface FileStorageService {

    /**
     * Uploads a file to the storage provider.
     */
    String uploadFile(MultipartFile file, String bucketName);

    /**
     * Retrieves an input stream for an object.
     */
    InputStream getFile(String bucketName, String objectName);

    /**
     * Extracts the object name from the full URL.
     */
    String extractObjectNameFromUrl(String url, String bucketName);

    /**
     * Helper to determine MIME content-type by extension.
     */
    static String determineContentTypeByExtension(String extension) {
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
}