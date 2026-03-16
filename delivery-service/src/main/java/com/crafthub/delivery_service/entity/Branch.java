package com.crafthub.delivery_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

/**
 * Entity representing a specific delivery branch/service point.
 */
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
    @JsonIgnore
    private Location location;

    @Column(nullable = false)
    private String externalId; // Carrier branch reference ID

    private String branchNumber; // Branch index, e.g., "1", "15-A"
    private String name; // Full branch name with address
}