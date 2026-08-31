package com.milhub.cart_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Version;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB document representing a user's shopping cart.
 * Carts are structured into sections by seller to facilitate checkout and
 * display.
 */
@Document(collection = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Cart {

    /**
     * Unique identifier for the cart, corresponding to the user ID.
     */
    @Id
    @ToString.Include
    private UUID userId;

    @Version
    private Long version;

    /**
     * List of cart sections, grouped by seller.
     */
    @Builder.Default
    private List<CartSection> sections = new ArrayList<>();

    /**
     * Total price of all items in the cart across all sections.
     */
    @Builder.Default
    @ToString.Include
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * Transient flag indicating if the cart data has been synchronized with the
     * Product Service.
     */
    @Transient
    @Builder.Default
    @ToString.Include
    private boolean isDataUpToDate = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return getUserId() != null && getUserId().equals(cart.getUserId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}