package com.crafthub.user_service.entity;

import com.crafthub.user_service.entity.enums.DocumentType;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "verification_docs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String docUrl; // Шлях до файлу в S3/MinIO

    @Enumerated(EnumType.STRING)
    private VerificationStatus status;

    private String rejectionReason;
}