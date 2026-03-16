package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.service.VerificationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing user verification documents (e.g., ID, certificates).
 */
@RestController
@RequestMapping("/api/v1/users/me/verification-docs")
@RequiredArgsConstructor
public class VerificationDocController {

    private final VerificationDocService docService;

    /**
     * Uploads a new verification document for the current user.
     */
    @PostMapping
    public ResponseEntity<VerificationResponseDTO> uploadDoc(@RequestBody VerificationDocRequestDTO dto) {
        return ResponseEntity.ok(docService.uploadDocument(dto));
    }

    /**
     * Retrieves all verification documents submitted by the current user.
     */
    @GetMapping
    public ResponseEntity<List<VerificationResponseDTO>> getMyDocs() {
        return ResponseEntity.ok(docService.getMyDocuments());
    }

    /**
     * Deletes a specific verification document by its ID.
     */
    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> deleteDoc(@PathVariable UUID docId) {
        docService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }
}