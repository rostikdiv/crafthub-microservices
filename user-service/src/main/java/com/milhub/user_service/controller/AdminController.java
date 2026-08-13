package com.milhub.user_service.controller;

import com.milhub.user_service.dto.admin.VerificationRequestResponseDTO;
import com.milhub.user_service.dto.admin.VerificationResponseDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.service.AdminService;
import com.milhub.user_service.service.VerificationDocService;
import com.milhub.user_service.service.UserService;
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
    private final UserService userService;

    @GetMapping("/verifications")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<List<VerificationRequestResponseDTO>> getPendingVerifications() {
        return ResponseEntity.ok(adminService.getPendingVerifications());
    }

    
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

   
    @PatchMapping("/docs/{id}/verify")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<String> verifyDoc(
            @PathVariable UUID id,
            @RequestParam boolean isApproved
    ) {
        docService.updateDocumentStatus(id, isApproved); 
        return ResponseEntity.ok("Document status updated");
    }

    
    @GetMapping("/users/{userId}/documents")
    @PreAuthorize("hasAuthority('user:verify')")
    public ResponseEntity<List<VerificationResponseDTO>> getUserDocuments(@PathVariable UUID userId) {
        return ResponseEntity.ok(docService.getDocumentsByUserId(userId));
    }

    @PostMapping("/users/{userId}/promote")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponseDTO> promoteToAdmin(@PathVariable UUID userId) {
        com.milhub.user_service.entity.User updatedUser = userService.promoteUserToAdmin(userId);
        return ResponseEntity.ok(userService.mapToResponseDTO(updatedUser));
    }
}