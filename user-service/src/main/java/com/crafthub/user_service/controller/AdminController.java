package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.admin.VerificationRequestResponseDTO; // 👈 Імпорт нового DTO
import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.service.AdminService;
import com.crafthub.user_service.service.VerificationDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final VerificationDocService docService;

    @GetMapping("/verifications")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<List<VerificationRequestResponseDTO>> getPendingVerifications() {
        return ResponseEntity.ok(adminService.getPendingVerifications());
    }

    // ✅ Цей метод приймає коментар (для емейлу)
    @PatchMapping("/users/{id}/verify")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<String> verifyUser(
            @PathVariable UUID id,
            @RequestParam boolean isVerified,
            @RequestParam(required = false) String reason
    ) {
        adminService.verifyUser(id, isVerified, reason);
        return ResponseEntity.ok("User verification status updated");
    }

    // Оновлений метод: використовує docService
    @PatchMapping("/docs/{id}/verify")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<String> verifyDoc(
            @PathVariable UUID id,
            @RequestParam boolean isApproved
    ) {
        docService.updateDocumentStatus(id, isApproved); // 👈 Виклик нового сервісу
        return ResponseEntity.ok("Document status updated");
    }

    // Оновлений метод: використовує docService
    @GetMapping("/users/{userId}/documents")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<List<VerificationResponseDTO>> getUserDocuments(@PathVariable UUID userId) {
        return ResponseEntity.ok(docService.getDocumentsByUserId(userId)); // 👈 Виклик нового сервісу
    }
}