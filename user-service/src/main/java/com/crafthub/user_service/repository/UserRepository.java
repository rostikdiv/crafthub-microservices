package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}