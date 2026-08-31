package com.milhub.delivery_service.entity;

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
@ToString(onlyExplicitlyIncluded = true)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @JsonIgnore
    private Location location;

    @Column(nullable = false)
    @ToString.Include
    private String externalId; // Carrier branch reference ID

    @ToString.Include
    private String branchNumber; // Branch index, e.g., "1", "15-A"
    @ToString.Include
    private String name; // Full branch name with address

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        Branch branch = (Branch) o;
        return getId() != null && getId().equals(branch.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}