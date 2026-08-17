package com.milhub.product_service.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Google Cloud Storage implementation of FileStorageService for product images.
 */
@Service
@Profile("cloud")
@Slf4j
public class GcsFileStorageService implements FileStorageService {

    private final Storage storage;

    public GcsFileStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        if (file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + extension;

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        log.info("Uploaded product image to GCS: gs://{}/{}", bucketName, fileName);
        return "https://storage.googleapis.com/" + bucketName + "/" + fileName;
    }

    @Override
    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null) return null;
        if (url.contains("/" + bucketName + "/")) {
            return url.substring(url.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
        }
        if (url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
