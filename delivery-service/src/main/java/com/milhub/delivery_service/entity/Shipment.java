package com.milhub.delivery_service.entity;

import com.milhub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.milhub.delivery_service.entity.enums.ShipmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a shipment record.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false) // Removed unique = true to allow multiple shipments (returns)
    @ToString.Include
    private UUID orderId; // Links to Order Service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private DeliveryStatus status; // PREPARING, SHIPPED, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    @ToString.Include
    private ShipmentType type = ShipmentType.OUTBOUND;

    @ToString.Include
    private String trackingNumber; // Tracking number (TTN)

    // Store a copy of the address for service autonomy
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private DeliveryDetailsDTO deliveryDetails;

    @ToString.Include
    private LocalDateTime createdAt;
    @ToString.Include
    private LocalDateTime shippedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = DeliveryStatus.PREPARING;
        if (this.type == null)
            this.type = ShipmentType.OUTBOUND;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Shipment shipment = (Shipment) o;
        return getId() != null && getId().equals(shipment.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}