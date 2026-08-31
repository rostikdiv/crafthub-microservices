package com.milhub.user_service.entity;

import com.milhub.user_service.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a system user.
 * Implements Spring Security's UserDetails for integration with authentication
 * mechanisms.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @ToString.Include
    private String firstName;

    @Column(nullable = false)
    @ToString.Include
    private String lastName;

    private String phoneNumber;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private Role role;

    @Column(nullable = false)
    @ToString.Include
    private Boolean isVerified = false;

    @Column(nullable = false, updatable = false)
    private Timestamp createdAt;

    private Timestamp updatedAt;

    // Relationships

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MilitaryProfile militaryProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SellerProfile sellerProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<VerificationDoc> documents;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SavedAddress> savedAddresses;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = this.createdAt;
        if (this.isVerified == null)
            this.isVerified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Spring Security UserDetails implementation

    /**
     * Returns granted authorities derived from the user's role and associated
     * permissions.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != org.hibernate.Hibernate.getClass(o)) return false;
        User user = (User) o;
        return getId() != null && getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}