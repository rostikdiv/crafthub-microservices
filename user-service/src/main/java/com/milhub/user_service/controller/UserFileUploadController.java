package com.milhub.user_service.controller;

import com.milhub.user_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
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

    // 1. Завантаження Аватарки (Бакет "avatars")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // Вантажимо в публічний бакет
        String url = fileStorageService.uploadFile(file, "avatars");
        return ResponseEntity.ok(Map.of("url", url));
    }

    // 2. Завантаження Документів для верифікації (Бакет "documents")
    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVerificationDocument(@RequestParam("file") MultipartFile file) {
        // Вантажимо в приватний бакет
        String url = fileStorageService.uploadFile(file, "documents");
        return ResponseEntity.ok(Map.of("url", url));
    }
}