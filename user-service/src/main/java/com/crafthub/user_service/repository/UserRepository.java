package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA автоматично згенерує SQL-запит за назвою методу!
    // "SELECT * FROM users WHERE email = ?"
    Optional<User> findByEmail(String email);
}