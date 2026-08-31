package com.milhub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

/**
 * Entity representing the extended profile information for a system user
 * registered as a seller.
 */
@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    @ToString.Include
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String taxId;

    @ToString.Include
    private Float rating;

    @Column(nullable = false)
    @Builder.Default
    @ToString.Include
    private Integer reviewCount = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    @ToString.Include
    private Integer totalSales = 0;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean autoConfirmOrders = true;

    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SellerPoint> pickupPoints;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        SellerProfile that = (SellerProfile) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}