package com.milhub.user_service.repository;

import com.milhub.user_service.entity.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email The email to search for.
     * @return An Optional containing the found User, or empty if not found.
     */
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"sellerProfile", "militaryProfile"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithProfiles(@org.springframework.data.repository.query.Param("id") UUID id);

    @EntityGraph(attributePaths = {"sellerProfile", "militaryProfile"})
    @Query("SELECT u FROM User u")
    java.util.List<User> findAllWithProfiles();
}