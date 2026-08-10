package com.milhub.user_service.service;

import com.milhub.user_service.dto.admin.VerificationResponseDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.VerificationDoc;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.repository.VerificationDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user verification documents, including upload, deletion,
 * and status updates by admins.
 */
@Service
@RequiredArgsConstructor
public class VerificationDocService {

    private final VerificationDocRepository docRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * Extracts the current authenticated user from the security context.
     */
    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // --- User Actions ---

    /**
     * Uploads and saves a new verification document for the user.
     * Limits the total number of documents per user.
     */
    @Transactional
    public VerificationResponseDTO uploadDocument(VerificationDocRequestDTO dto) {
        User user = getCurrentUser();

        if (user.getDocuments().size() >= 10) {
            throw new BusinessException("Limit of documents exceeded");
        }

        VerificationDoc doc = VerificationDoc.builder()
                .user(user)
                .documentType(dto.documentType())
                .docUrl(dto.docUrl())
                .status(VerificationStatus.PENDING)
                .build();

        doc = docRepository.save(doc);
        return mapToDTO(doc);
    }

    /**
     * Retrieves all verification documents belonging to the current user.
     */
    @Transactional(readOnly = true)
    public List<VerificationResponseDTO> getMyDocuments() {
        User user = getCurrentUser();
        return docRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a verification document, provided the user owns it and it hasn't been
     * approved yet.
     */
    @Transactional
    public void deleteDocument(UUID docId) {
        User user = getCurrentUser();
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }

        if (doc.getStatus() == VerificationStatus.APPROVED) {
            throw new BusinessException("Cannot delete approved document");
        }

        docRepository.delete(doc);
    }

    // --- Admin Actions ---

    /**
     * Retrieves all documents for a specific user. Restricted to admins.
     */
    @Transactional(readOnly = true)
    public List<VerificationResponseDTO> getDocumentsByUserId(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return docRepository.findAllByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates the verification status of a document.
     */
    @Transactional
    public void updateDocumentStatus(UUID docId, boolean isApproved) {
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));

        if (isApproved) {
            doc.setStatus(VerificationStatus.APPROVED);
        } else {
            doc.setStatus(VerificationStatus.REJECTED);
        }
        docRepository.save(doc);
    }

    /**
     * Downloads the document file from storage.
     * Accessible by the owner or an administrator.
     */
    @Transactional(readOnly = true)
    public InputStream downloadDocument(UUID docId) {
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        User user = getCurrentUser();
        boolean isOwner = doc.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.milhub.user_service.entity.enums.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new BusinessException("Access denied");
        }

        String objectName = fileStorageService.extractObjectNameFromUrl(doc.getDocUrl(), "documents");
        if (objectName == null) {
            throw new ResourceNotFoundException("File not found in storage");
        }

        return fileStorageService.getFile("documents", objectName);
    }

    /**
     * Maps a VerificationDoc entity to a DTO, including a proxy URL for
     * downloading.
     */
    private VerificationResponseDTO mapToDTO(VerificationDoc doc) {
        String proxyUrl = "/api/v1/documents/" + doc.getId();

        return new VerificationResponseDTO(
                doc.getId(),
                doc.getUser().getId(),
                doc.getDocumentType(),
                proxyUrl,
                doc.getStatus(),
                doc.getCreatedAt());
    }
}