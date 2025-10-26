package com.crafthub.product_service.repository;

import com.crafthub.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // CRUD-операції вже готові "з коробки"
}