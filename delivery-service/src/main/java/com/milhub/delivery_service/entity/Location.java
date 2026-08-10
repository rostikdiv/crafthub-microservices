package com.milhub.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity representing a geographic location supported by delivery providers.
 */
@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_location_provider_name", columnList = "provider, nameUkr")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryProvider provider; // Support for multiple providers

    @Column(nullable = false)
    private String externalId; // Carrier-specific ID (Reference)

    @Column(nullable = false)
    private String nameUkr; // Ukrainian name of the location

    private String region; // Region/Oblast name

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Branch> branches = new java.util.ArrayList<>();
}