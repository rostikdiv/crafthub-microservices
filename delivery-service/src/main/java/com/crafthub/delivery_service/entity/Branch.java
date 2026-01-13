package com.crafthub.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branch_location", columnList = "location_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private String externalId; // Ref відділення

    private String branchNumber; // "1", "15-A"
    private String name; // "Відділення №1: вул. Городоцька..."
}