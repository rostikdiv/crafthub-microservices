package com.crafthub.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(nullable = false)
    private UUID subOrderId; // Прив'язка до конкретного продавця в замовленні

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryProvider provider; // NOVA_POSHTA, UKRPOSHTA

    private String trackingNumber; // ТТН

    @Column(columnDefinition = "TEXT")
    private String senderInfo; // JSON з даними відправника (спрощено для MVP)

    @Column(columnDefinition = "TEXT")
    private String recipientInfo; // JSON з даними отримувача

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    private String labelUrl; // Посилання на PDF накладну

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

enum DeliveryProvider { NOVA_POSHTA, UKRPOSHTA }
enum DeliveryStatus { CREATED, IN_TRANSIT, ARRIVED, RECEIVED, RETURNED }