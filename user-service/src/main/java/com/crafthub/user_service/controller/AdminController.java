package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Якщо налаштуєте ролі
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // PATCH /api/v1/admin/users/{id}/verify?isVerified=true
    @PatchMapping("/users/{id}/verify")
    public ResponseEntity<String> verifyUser(@PathVariable UUID id, @RequestParam boolean isVerified) {
        adminService.verifyUser(id, isVerified);
        return ResponseEntity.ok("User verification status updated to " + isVerified);
    }

    // PATCH /api/v1/admin/docs/{id}/verify?isApproved=true
    @PatchMapping("/docs/{id}/verify")
    public ResponseEntity<String> verifyDoc(
            @PathVariable UUID id,
            @RequestParam boolean isApproved,
            @RequestParam(required = false) String reason
    ) {
        adminService.verifyDocument(id, isApproved, reason);
        return ResponseEntity.ok("Document status updated");
    }

    /**
     * Отримати всі документи користувача для перевірки.
     * Доступ: Тільки користувачі з правом 'user:verify' (тобто ADMIN).
     */
    @GetMapping("/users/{userId}/documents")
    @PreAuthorize("hasAuthority('user:verify')") // 🔐 Перевірка права (Permission)
    public ResponseEntity<List<VerificationResponseDTO>> getUserDocuments(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUserDocuments(userId));
    }
}