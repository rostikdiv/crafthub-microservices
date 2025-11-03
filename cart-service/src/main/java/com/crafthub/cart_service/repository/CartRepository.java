package com.crafthub.cart_service.repository;

import com.crafthub.cart_service.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
// ❗️ Cart - тип документа, String - тип ID (наш userId)
public interface CartRepository extends MongoRepository<Cart, String> {
    // Spring Data Mongo автоматично надасть нам:
    // - findById(String userId) -> Optional<Cart>
    // - save(Cart cart)
    // - deleteById(String userId)
}