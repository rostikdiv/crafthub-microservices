package com.milhub.user_service.controller;

import com.milhub.user_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/uploads")
@RequiredArgsConstructor
public class UserFileUploadController {

    private final FileStorageService fileStorageService;

    @Value("${minio.bucket.avatars:avatars}")
    private String avatarsBucket;

    @Value("${minio.bucket.documents:documents}")
    private String documentsBucket;

    // 1. Upload Avatar
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, avatarsBucket);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // 2. Upload Documents for Verification
    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVerificationDocument(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadFile(file, documentsBucket);
        return ResponseEntity.ok(Map.of("url", url));
    }
}