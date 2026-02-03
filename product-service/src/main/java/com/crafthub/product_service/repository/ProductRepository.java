package com.crafthub.product_service.repository;

import com.crafthub.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // ✅ Додано
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    // JpaSpecificationExecutor додає методи findAll(Specification, Pageable)
}