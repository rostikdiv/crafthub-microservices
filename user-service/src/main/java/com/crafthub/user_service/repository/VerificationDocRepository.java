package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VerificationDocRepository extends JpaRepository<VerificationDoc, UUID> {
    List<VerificationDoc> findAllByStatus(VerificationStatus status);

    // Знайти всі документи конкретного юзера (може знадобитися пізніше)
    List<VerificationDoc> findAllByUserId(UUID userId);


}