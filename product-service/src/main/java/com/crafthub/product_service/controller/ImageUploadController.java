package com.crafthub.product_service.controller;

import com.crafthub.product_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller for handling product image uploads.
 */
@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Uploads a single product image to storage.
     *
     * @param file the multipart file to upload
     * @return the public URL of the uploaded image
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        // Upload to the "products" bucket
        String url = fileStorageService.uploadFile(file, "products");
        return ResponseEntity.ok(Map.of("url", url));
    }
}