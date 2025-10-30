package com.crafthub.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId; // Зберігаємо ID продукту з product-service

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit; // Ціна *на момент покупки*

    // ❗️ Зв'язок: Багато OrderItems належать одному Order
    // @JoinColumn: Ми керуємо зв'язком з цього боку (у нас буде 'order_id')
    @ManyToOne(fetch = FetchType.LAZY) // LAZY - не завантажувати Order, поки не попросимо
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}