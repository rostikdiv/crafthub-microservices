package com.crafthub.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing an individual item within an order.
 */
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
    private UUID productId;

    @Column(nullable = true)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal pricePerUnit; // Item price at the time of purchase

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;
}