package com.crafthub.product_service.controller;

import com.crafthub.product_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final FileStorageService fileStorageService;

    // Завантаження одного фото
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        // Вантажимо в бакет "products"
        String url = fileStorageService.uploadFile(file, "products");
        return ResponseEntity.ok(Map.of("url", url));
    }
}