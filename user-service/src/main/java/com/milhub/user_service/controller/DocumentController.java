package com.milhub.user_service.controller;

import com.milhub.user_service.service.VerificationDocService;
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

/**
 * Controller for serving secure document resources via a proxy stream.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final VerificationDocService docService;

    /**
     * Streams the content of a verification document based on its ID.
     * Effectively acts as a proxy between the storage and the client.
     */
    @GetMapping("/{docId}")
    public ResponseEntity<Resource> getDocument(@PathVariable UUID docId) {
        VerificationDocService.DocumentDownloadDTO download = docService.downloadDocument(docId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }
}
