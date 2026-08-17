package com.milhub.product_service.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage operations across environments.
 */
public interface FileStorageService {

    /**
     * Uploads a file to storage and returns the access URL.
     */
    String uploadFile(MultipartFile file, String bucketName);

    /**
     * Helper to extract the object name from URL.
     */
    String extractObjectNameFromUrl(String url, String bucketName);
}