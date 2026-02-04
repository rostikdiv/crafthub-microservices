package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.UserRepository;
import com.crafthub.user_service.repository.VerificationDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VerificationDocService {

    private final VerificationDocRepository docRepository;
    private final UserRepository userRepository;

    // --- HELPER: Отримати поточного юзера ---
    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ===========================
    // USER ACTIONS (CRUD)
    // ===========================

    // [C] CREATE: Завантажити документ
    @Transactional
    public VerificationResponseDTO uploadDocument(VerificationDocRequestDTO dto) {
        User user = getCurrentUser();

        // Можна додати обмеження: не більше 10 документів
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

    // [R] READ: Отримати мої документи
    @Transactional(readOnly = true)
    public List<VerificationResponseDTO> getMyDocuments() {
        User user = getCurrentUser();
        return docRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // [D] DELETE: Видалити документ
    @Transactional
    public void deleteDocument(UUID docId) {
        User user = getCurrentUser();
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Перевірка власника: чи належить документ поточному юзеру?
        if (!doc.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }

        // Не можна видаляти вже підтверджені документи (бізнес-правило)
        if (doc.getStatus() == VerificationStatus.APPROVED) {
            throw new BusinessException("Cannot delete approved document");
        }

        docRepository.delete(doc);
    }

    // ===========================
    // ADMIN ACTIONS
    // ===========================

    // [R] READ ALL (By User ID)
    @Transactional(readOnly = true)
    public List<VerificationResponseDTO> getDocumentsByUserId(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return docRepository.findAllByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // [U] UPDATE STATUS (Verify)
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

    // --- MAPPER ---
    private VerificationResponseDTO mapToDTO(VerificationDoc doc) {
        return new VerificationResponseDTO(
                doc.getId(),
                doc.getUser().getId(),
                doc.getDocumentType(),
                doc.getDocUrl(),
                doc.getStatus(),
                doc.getCreatedAt()
        );
    }
}