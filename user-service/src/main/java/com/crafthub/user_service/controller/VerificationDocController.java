package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.service.VerificationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/verification-docs") // Винесли в окремий URL
@RequiredArgsConstructor
public class VerificationDocController {

    private final VerificationDocService docService;

    // [C] Додати документ
    @PostMapping
    public ResponseEntity<VerificationResponseDTO> uploadDoc(@RequestBody VerificationDocRequestDTO dto) {
        return ResponseEntity.ok(docService.uploadDocument(dto));
    }

    // [R] Отримати список моїх документів
    @GetMapping
    public ResponseEntity<List<VerificationResponseDTO>> getMyDocs() {
        return ResponseEntity.ok(docService.getMyDocuments());
    }

    // [D] Видалити документ (наприклад, якщо завантажив помилково)
    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> deleteDoc(@PathVariable UUID docId) {
        docService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }
}