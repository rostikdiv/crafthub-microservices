package com.milhub.user_service.entity;

import com.milhub.user_service.entity.enums.DeliveryProvider;
import com.milhub.user_service.entity.enums.DeliveryType;
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
@ToString(onlyExplicitlyIncluded = true)
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ToString.Include
    private String title; // e.g., "Home", "Work"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private DeliveryProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private DeliveryType deliveryType;

    // Location details
    private String cityRef;
    @ToString.Include
    private String cityName;
    @ToString.Include
    private String region;

    // Branch-specific details
    private String branchRef;
    @ToString.Include
    private String branchName;

    // Courier delivery details
    @ToString.Include
    private String streetName;
    @ToString.Include
    private String building;
    private String apartment;
    private String zipCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        SavedAddress that = (SavedAddress) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}