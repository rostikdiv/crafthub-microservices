package com.crafthub.delivery_service.entity;

import com.crafthub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.crafthub.delivery_service.entity.enums.ShipmentType;
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
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) // Removed unique = true to allow multiple shipments (returns)
    private UUID orderId; // Links to Order Service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status; // PREPARING, SHIPPED, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShipmentType type = ShipmentType.OUTBOUND;

    private String trackingNumber; // Tracking number (TTN)

    // Store a copy of the address for service autonomy
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private DeliveryDetailsDTO deliveryDetails;

    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = DeliveryStatus.PREPARING;
        if (this.type == null)
            this.type = ShipmentType.OUTBOUND;
    }
}