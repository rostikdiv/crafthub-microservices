package com.crafthub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

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
    private SellerProfile sellerProfile;

    @Column(nullable = false)
    private String name; // "Головна майстерня"

    // --- ✅ НОВІ СТРУКТУРОВАНІ ПОЛЯ ---

    // Прив'язка до міста (важливо для фільтрів!)
    private String cityRef;
    private String cityName;
    private String region;

    // Фізична адреса
    private String streetName;
    private String building;
    private String apartment;
    private String zipCode;

    // Контакти
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String instructions;
}