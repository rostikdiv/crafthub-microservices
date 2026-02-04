package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.UserVerificationEvent;
import com.crafthub.user_service.dto.admin.VerificationRequestResponseDTO;
import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.UserRepository;
import com.crafthub.user_service.repository.VerificationDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<VerificationRequestResponseDTO> getPendingVerifications() {
        return userRepository.findAll().stream()
                .filter(user -> !Boolean.TRUE.equals(user.getIsVerified())) // Тільки не верифіковані
                .filter(this::hasPendingDocs)         // Тільки ті, хто має документи на перевірці
                .map(this::mapToRequestDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void verifyUser(UUID userId, boolean isVerified, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setIsVerified(isVerified);

        if (isVerified) {
            // Підвищення ролі (без змін)
            if (user.getMilitaryProfile() != null) {
                log.info("Upgrading User {} to MILITARY_UNIT", userId);
                user.setRole(Role.MILITARY_UNIT);
            } else if (user.getSellerProfile() != null) {
                log.info("Upgrading User {} to SELLER", userId);
                user.setRole(Role.SELLER);
            }
        } else {
            log.info("User {} verification rejected. Reason: {}", userId, reason);
        }

        userRepository.save(user);

        // 🔥 ВІДПРАВКА PODIЇ В KAFKA
        try {
            UserVerificationEvent event = new UserVerificationEvent(
                    user.getId(),
                    user.getEmail(),
                    isVerified,
                    reason
            );
            kafkaTemplate.send("user-verification-topic", event); // 👈 Відправляємо в топік
            log.info("Sent verification event for user {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send Kafka event", e);
            // Не кидаємо помилку, щоб не відкочувати транзакцію верифікації
        }
    }

    private boolean hasPendingDocs(User user) {
        if (user.getDocuments() == null || user.getDocuments().isEmpty()) return false;
        return user.getDocuments().stream()
                .anyMatch(doc -> doc.getStatus() == VerificationStatus.PENDING);
    }

    private VerificationRequestResponseDTO mapToRequestDTO(User user) {
        String specificName = "N/A";
        if (user.getSellerProfile() != null) {
            specificName = user.getSellerProfile().getCompanyName();
        } else if (user.getMilitaryProfile() != null) {
            specificName = user.getMilitaryProfile().getUnitNumber();
        }

        long pendingCount = user.getDocuments().stream()
                .filter(d -> d.getStatus() == VerificationStatus.PENDING)
                .count();

        return new VerificationRequestResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getRole(),
                specificName,
                user.getCreatedAt().toLocalDateTime(),
                pendingCount
        );
    }

    private VerificationResponseDTO mapToDocDTO(VerificationDoc doc) {
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