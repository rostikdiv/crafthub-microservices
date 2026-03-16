package com.crafthub.user_service.entity;

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
public class SellerPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    @JsonIgnore
    private SellerProfile sellerProfile;

    @Column(nullable = false)
    private String name;

    // Location references and city data
    private String cityRef;
    private String cityName;
    private String region;

    // Physical address details
    private String streetName;
    private String building;
    private String apartment;
    private String zipCode;

    // Contact information
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String instructions;
}