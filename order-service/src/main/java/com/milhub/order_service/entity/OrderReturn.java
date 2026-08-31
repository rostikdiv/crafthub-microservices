package com.milhub.order_service.entity;

import com.milhub.order_service.entity.enums.ReturnReason;
import com.milhub.order_service.entity.enums.ReturnStatus;
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
@ToString(onlyExplicitlyIncluded = true)
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    // Link to the order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    // ID of the specific OrderItem being returned (MVP simplification: one item per
    // request)
    @Column(nullable = false)
    @ToString.Include
    private Long orderItemId;

    @Column(nullable = false)
    @ToString.Include
    private UUID productId;

    @Column(nullable = false)
    @ToString.Include
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private ReturnStatus status;

    // --- Financial Information ---

    // Price at which the item was purchased (including discounts)
    @Column(nullable = false)
    private BigDecimal itemPriceSnapshot;

    // Return shipping cost (if applicable)
    private BigDecimal returnShippingCost;

    // Final refund amount to be paid back
    @Column(nullable = false)
    @ToString.Include
    private BigDecimal finalRefundAmount;

    // Whether shipping costs were deducted from the refund amount
    @Builder.Default
    private boolean isShippingDeducted = false;

    // Tracking number for return shipment (from Delivery Service)
    @ToString.Include
    private String returnTrackingNumber;
    private UUID returnShipmentId;

    @ToString.Include
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        OrderReturn that = (OrderReturn) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
