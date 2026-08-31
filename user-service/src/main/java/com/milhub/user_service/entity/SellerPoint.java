package com.milhub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

/**
 * Entity representing a physical pickup point managed by a seller.
 */
@Entity
@Table(name = "seller_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class SellerPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    @JsonIgnore
    private SellerProfile sellerProfile;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    // Location references and city data
    private String cityRef;
    @ToString.Include
    private String cityName;
    @ToString.Include
    private String region;

    // Physical address details
    @ToString.Include
    private String streetName;
    @ToString.Include
    private String building;
    private String apartment;
    private String zipCode;

    // Contact information
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        SellerPoint that = (SellerPoint) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}