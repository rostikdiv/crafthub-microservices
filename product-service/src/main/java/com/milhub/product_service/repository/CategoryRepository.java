package com.milhub.product_service.repository;

import com.milhub.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> { // ✅ Long
    Optional<Category> findByName(String name);
}