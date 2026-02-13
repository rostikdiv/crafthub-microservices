package com.crafthub.user_service.controller;

import com.crafthub.user_service.service.VerificationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final VerificationDocService docService;

    @GetMapping("/{docId}")
    public ResponseEntity<Resource> getDocument(@PathVariable UUID docId) {
        InputStream fileStream = docService.downloadDocument(docId);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // Default to JPEG, browser will often sniff or we can improve later
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.jpg\"")
                .body(new InputStreamResource(fileStream));
    }
}
