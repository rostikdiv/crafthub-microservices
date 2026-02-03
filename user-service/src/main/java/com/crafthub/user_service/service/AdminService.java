package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import com.crafthub.user_service.exception.ResourceNotFoundException; // ✅
import com.crafthub.user_service.repository.UserRepository;
import com.crafthub.user_service.repository.VerificationDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final VerificationDocRepository docRepository;

    @Transactional
    public void verifyUser(UUID userId, boolean isVerified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setIsVerified(isVerified);

        if (isVerified) {
            if (user.getMilitaryProfile() != null) {
                log.info("Upgrading User {} to MILITARY_UNIT", userId);
                user.setRole(Role.MILITARY_UNIT);
            } else if (user.getSellerProfile() != null) {
                log.info("Upgrading User {} to SELLER", userId);
                user.setRole(Role.SELLER);
            } else {
                log.warn("User {} verified but has no specific profile requests", userId);
            }
        }
        userRepository.save(user);
    }

    @Transactional
    public void verifyDocument(UUID docId, boolean isApproved, String rejectionReason) {
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));

        if (isApproved) {
            doc.setStatus(VerificationStatus.APPROVED);
            doc.setRejectionReason(null);
        } else {
            doc.setStatus(VerificationStatus.REJECTED);
            doc.setRejectionReason(rejectionReason);
        }
        docRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<VerificationResponseDTO> getUserDocuments(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        return docRepository.findAllByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private VerificationResponseDTO mapToDTO(VerificationDoc doc) {
        return new VerificationResponseDTO(
                doc.getId(),
                doc.getUser().getId(),
                doc.getDocumentType(),
                doc.getDocUrl(),
                doc.getStatus(),
                doc.getRejectionReason(),
                null
        );
    }
}