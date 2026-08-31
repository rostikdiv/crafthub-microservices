package com.milhub.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a financial transaction in the system.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private UUID orderId;

    @Column(nullable = false)
    @ToString.Include
    private UUID userId;

    @Column(nullable = false)
    @ToString.Include
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private TransactionStatus status;

    @ToString.Include
    private String provider; // e.g., STRIPE, LIQPAY, MOCK_PAY

    @Column(unique = true)
    @ToString.Include
    private String idempotencyKey;

    @Version
    private Long version;

    @CreationTimestamp
    @ToString.Include
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Transaction that = (Transaction) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}