package com.crafthub.cart_service.entity;

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
public class Cart {

    /**
     * Unique identifier for the cart, corresponding to the user ID.
     */
    @Id
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
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /**
     * Transient flag indicating if the cart data has been synchronized with the
     * Product Service.
     */
    @Transient
    @Builder.Default
    private boolean isDataUpToDate = true;
}