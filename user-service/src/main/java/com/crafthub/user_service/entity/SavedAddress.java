package com.crafthub.user_service.entity;

import com.crafthub.user_service.entity.enums.DeliveryProvider;
import com.crafthub.user_service.entity.enums.DeliveryType;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

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
    private User user;

    private String title; // "Дім", "Робота"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType deliveryType;

    // Дані локації (копіюємо з Delivery Service)
    private String cityRef;
    private String cityName;
    private String region;

    // Для відділення
    private String branchRef;
    private String branchName;

    // Для кур'єра
    private String streetName;
    private String building;
    private String apartment;
    private String zipCode;
}