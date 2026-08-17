package com.milhub.user_service.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO implementation of FileStorageService for local/docker environments.
 */
@Service
@Profile("!cloud")
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.external-url:http://localhost:9000}")
    private String externalUrl;

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        if (file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + extension;

        InputStream inputStream = file.getInputStream();

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception ignored) {}

        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty() || "application/octet-stream".equals(contentType)) {
            contentType = FileStorageService.determineContentTypeByExtension(extension);
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());

        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    @Override
    @SneakyThrows
    public InputStream getFile(String bucketName, String objectName) {
        return minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    @Override
    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null) return null;

        if (url.contains("/" + bucketName + "/")) {
            return url.substring(url.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
        }

        if (url.contains(bucketName)) {
            return url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
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
