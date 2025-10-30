package com.crafthub.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders") // Використовуємо 'orders', оскільки 'order' - ключове слово SQL
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private String orderNumber; // ❗️ Бізнес-ключ, не покладаємося на ID

    @Column(nullable = false)
    private String userId; // ❗️ Ми не будемо використовувати FK до іншої БД

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ❗️ Зв'язок: Один Order має багато OrderItems
    // CascadeType.ALL: Якщо ми видаляємо Order, видаляються і всі його Items.
    // orphanRemoval = true: Якщо ми видаляємо Item зі списку, він видаляється з БД.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING; // Початковий статус
    }
}