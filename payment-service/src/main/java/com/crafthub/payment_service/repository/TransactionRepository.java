package com.crafthub.payment_service.repository;

import com.crafthub.payment_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Transaction entities in the database.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Finds a transaction associated with a specific order ID.
     */
    Optional<Transaction> findByOrderId(UUID orderId);

    /**
     * Finds a transaction associated with a specific idempotency key.
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}