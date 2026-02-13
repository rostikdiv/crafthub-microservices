package com.crafthub.user_service.service;

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

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    // Removed injected signing client to prevent startup connection attempts

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.external-url:http://localhost:9000}")
    private String externalUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @SneakyThrows
    public String uploadFile(MultipartFile file, String bucketName) {
        if (file.isEmpty()) {
            throw new RuntimeException("File must not be empty");
        }

        // 1. Генеруємо унікальне ім'я
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + extension;

        // 2. Отримуємо потік
        InputStream inputStream = file.getInputStream();

        // 3. Перевіряємо та створюємо бакет
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 4. Вантажимо в MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        // 4. Повертаємо URL (зовнішній / documents / file)
        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    @SneakyThrows
    public String getPresignedUrl(String objectName, String bucketName) {
        // Use the internal client (minio:9000) which is guaranteed to work.
        // The URL will be http://minio:9000/...
        // The frontend (fixImageUrl) handles the rewrite to localhost:9000 for the
        // browser.
        return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(24 * 60 * 60) // 24 hours
                        .build());
    }

    public String extractObjectNameFromUrl(String url, String bucketName) {
        if (url == null || !url.contains(bucketName))
            return null;
        // Example: http://minio:9000/documents/uuid.jpg
        // bucketName = documents
        // We want uuid.jpg
        String afterBucket = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
        return afterBucket;
    }

    @SneakyThrows
    public InputStream getFile(String bucketName, String objectName) {
        return minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}