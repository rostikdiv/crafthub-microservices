package com.milhub.user_service.entity;

import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a document submitted for user verification purposes.
 */
@Entity
@Table(name = "verification_docs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class VerificationDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private DocumentType documentType;

    private String docUrl;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private VerificationStatus status;

    @ToString.Include
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        VerificationDoc that = (VerificationDoc) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}