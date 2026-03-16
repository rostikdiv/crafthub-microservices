package com.crafthub.order_service.entity;

import com.crafthub.order_service.entity.enums.ReturnReason;
import com.crafthub.order_service.entity.enums.ReturnStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a product return request for an order.
 */
@Entity
@Table(name = "order_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Link to the order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    // ID of the specific OrderItem being returned (MVP simplification: one item per
    // request)
    @Column(nullable = false)
    private Long orderItemId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    // --- Financial Information ---

    // Price at which the item was purchased (including discounts)
    @Column(nullable = false)
    private BigDecimal itemPriceSnapshot;

    // Return shipping cost (if applicable)
    private BigDecimal returnShippingCost;

    // Final refund amount to be paid back
    @Column(nullable = false)
    private BigDecimal finalRefundAmount;

    // Whether shipping costs were deducted from the refund amount
    @Builder.Default
    private boolean isShippingDeducted = false;

    // Tracking number for return shipment (from Delivery Service)
    private String returnTrackingNumber;
    private UUID returnShipmentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = ReturnStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
