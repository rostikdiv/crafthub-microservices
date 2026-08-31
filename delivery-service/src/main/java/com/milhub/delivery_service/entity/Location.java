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
@ToString(onlyExplicitlyIncluded = true)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private DeliveryProvider provider; // Support for multiple providers

    @Column(nullable = false)
    @ToString.Include
    private String externalId; // Carrier-specific ID (Reference)

    @Column(nullable = false)
    @ToString.Include
    private String nameUkr; // Ukrainian name of the location

    @ToString.Include
    private String region; // Region/Oblast name

    @Builder.Default
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Branch> branches = new java.util.ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Location location = (Location) o;
        return getId() != null && getId().equals(location.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}