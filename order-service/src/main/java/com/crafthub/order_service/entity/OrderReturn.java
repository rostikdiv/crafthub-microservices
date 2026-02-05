package com.crafthub.order_service.entity;

import com.crafthub.order_service.entity.enums.ReturnReason;
import com.crafthub.order_service.entity.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    // Зв'язок з замовленням
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ID конкретного OrderItem, який повертають (спрощення: повертаємо по одній
    // позиції)
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

    // --- Фінансова інформація ---

    // Ціна, за яку товар купували (з врахуванням знижок на момент покупки)
    @Column(nullable = false)
    private BigDecimal itemPriceSnapshot;

    // Вартість зворотної доставки (якщо є)
    private BigDecimal returnShippingCost;

    // Фінальна сума до виплати
    @Column(nullable = false)
    private BigDecimal finalRefundAmount;

    // Чи було знято кошти за доставку з суми повернення
    @Builder.Default
    private boolean isShippingDeducted = false;

    // ТТН зворотної доставки (з Delivery Service)
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
