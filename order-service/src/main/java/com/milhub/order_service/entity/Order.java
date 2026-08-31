package com.milhub.order_service.entity;

import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an order.
 * Stores information about the user, seller, items, status, and delivery
 * details.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false)
    @ToString.Include
    private UUID userId;

    @Column(nullable = false)
    @ToString.Include
    private UUID sellerId;

    @Column(nullable = false)
    @ToString.Include
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private com.milhub.order_service.entity.enums.PaymentMethod paymentMethod;

    @ToString.Include
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private DeliveryDetailsDTO deliveryInfo;

    @Column(length = 1000)
    private String returnReason;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)
            this.status = OrderStatus.PENDING_PAYMENT;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Order order = (Order) o;
        return getId() != null && getId().equals(order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}