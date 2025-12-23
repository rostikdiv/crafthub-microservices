package com.crafthub.product_service.repository;

import com.crafthub.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID; // ✅ Важливий імпорт

// ✅ Змінили Long на UUID
public interface ProductRepository extends JpaRepository<Product, UUID> {
    // Тут поки що не потрібні додаткові методи, бо ми вирішили показувати всі товари.
    // Стандартного findAll() вистачить.
}