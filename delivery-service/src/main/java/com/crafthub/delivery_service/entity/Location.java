package com.crafthub.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
    private DeliveryProvider provider; // NOVA_POSHTA або UKRPOSHTA

    @Column(nullable = false)
    private String externalId; // ID в базі перевізника (Ref)

    @Column(nullable = false)
    private String nameUkr; // "Львів"

    private String region; // "Львівська область"

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Branch> branches = new java.util.ArrayList<>();
}