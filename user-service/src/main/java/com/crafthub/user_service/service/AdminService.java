package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.admin.VerificationResponseDTO;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import com.crafthub.user_service.repository.UserRepository;
import com.crafthub.user_service.repository.VerificationDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final VerificationDocRepository docRepository;

    // ✅ ОСНОВНА ЗМІНА: Апгрейд ролі при верифікації
    @Transactional
    public void verifyUser(UUID userId, boolean isVerified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setIsVerified(isVerified);

        if (isVerified) {
            // Якщо адмін підтвердив користувача, перевіряємо, на яку роль він подавався

            if (user.getMilitaryProfile() != null) {
                log.info("Upgrading User {} to MILITARY_UNIT", userId);
                user.setRole(Role.MILITARY_UNIT);
            }
            else if (user.getSellerProfile() != null) {
                log.info("Upgrading User {} to SELLER", userId);
                user.setRole(Role.SELLER);
            }
            else {
                // Якщо профілів немає, але адмін чомусь верифікував - залишаємо як є (або логуємо warning)
                log.warn("User {} verified but has no specific profile requests", userId);
            }
        } else {
            // ОПЦІОНАЛЬНО: Якщо адмін зняв верифікацію, можна понизити роль назад до BUYER
            // user.setRole(Role.BUYER);
        }

        userRepository.save(user);
    }

    // Затвердити або відхилити документ (Без змін)
    @Transactional
    public void verifyDocument(UUID docId, boolean isApproved, String rejectionReason) {
        VerificationDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

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
        // Перевіряємо, чи існує такий юзер (опціонально, але бажано)
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // Використовуємо метод репозиторію, який вже є у файлі
        return docRepository.findAllByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private VerificationResponseDTO mapToDTO(VerificationDoc doc) {
        // У VerificationDoc немає поля createdAt, тому поки передаємо null або додайте це поле в сутність
        // Якщо ви використовуєте @CreationTimestamp у сутності, то getter буде доступний.
        return new VerificationResponseDTO(
                doc.getId(),
                doc.getUser().getId(),
                doc.getDocumentType(),
                doc.getDocUrl(),
                doc.getStatus(),
                doc.getRejectionReason(),
                null // або doc.getCreatedAt(), якщо додасте це поле в Entity
        );
    }
}