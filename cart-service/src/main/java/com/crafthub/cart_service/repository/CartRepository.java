package com.crafthub.cart_service.repository;

import com.crafthub.cart_service.entity.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
// ❗️ Cart - тип документа, String - тип ID (наш userId)
public interface CartRepository extends MongoRepository<Cart, UUID> {
    // Spring Data Mongo автоматично надасть нам:
    // - findById(String userId) -> Optional<Cart>
    // - save(Cart cart)
    // - deleteById(String userId)
}