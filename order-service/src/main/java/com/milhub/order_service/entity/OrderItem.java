package com.milhub.order_service.entity;

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
@ToString(onlyExplicitlyIncluded = true)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false)
    @ToString.Include
    private UUID productId;

    @Column(nullable = true)
    @ToString.Include
    private String name;

    @Column(nullable = false)
    @ToString.Include
    private Integer quantity;

    @Column(nullable = false)
    @ToString.Include
    private BigDecimal pricePerUnit; // Item price at the time of purchase

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        OrderItem orderItem = (OrderItem) o;
        return getId() != null && getId().equals(orderItem.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}