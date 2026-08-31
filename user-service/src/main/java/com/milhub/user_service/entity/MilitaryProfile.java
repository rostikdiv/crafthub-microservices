package com.milhub.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

/**
 * Entity representing the extended profile information for a system user
 * registered as military personnel.
 */
@Entity
@Table(name = "military_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class MilitaryProfile {

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
    private String unitNumber;

    @Column(nullable = false)
    @ToString.Include
    private String edrpou;

    @ToString.Include
    private String commanderName;

    @ToString.Include
    private String officialAddress;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        MilitaryProfile that = (MilitaryProfile) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}