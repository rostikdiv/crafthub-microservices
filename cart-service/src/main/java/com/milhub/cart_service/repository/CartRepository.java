package com.milhub.cart_service.repository;

import com.milhub.cart_service.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for MongoDB Cart operations.
 * Uses the user's UUID as the primary document ID.
 */
@Repository
public interface CartRepository extends MongoRepository<Cart, UUID> {
    // Standard MongoRepository methods are available for Cart management.
}