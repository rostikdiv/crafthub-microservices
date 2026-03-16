package com.crafthub.user_service.entity;

import com.crafthub.user_service.entity.enums.DeliveryProvider;
import com.crafthub.user_service.entity.enums.DeliveryType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Entity representing an address saved by a user for future deliveries.
 */
@Entity
@Table(name = "saved_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String title; // e.g., "Home", "Work"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType;

    // Location details
    private String cityRef;
    private String cityName;
    private String region;

    // Branch-specific details
    private String branchRef;
    private String branchName;

    // Courier delivery details
    private String streetName;
    private String building;
    private String apartment;
    private String zipCode;
}