package com.crafthub.product_service.service;

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

    @Value("${minio.url}")
    private String minioUrl;

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

        // 3. Вантажимо в MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 4. Повертаємо URL
        return minioUrl + "/" + bucketName + "/" + fileName;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}