package com.milhub.product_service.service;

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
 * MinIO implementation of FileStorageService for product images.
 */
@Service
@Profile("!cloud")
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.url:http://localhost:9000}")
    private String minioUrl;

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

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    @Override
    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null || !url.contains(bucketName))
            return null;
        return url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
