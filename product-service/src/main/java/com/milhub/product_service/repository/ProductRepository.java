package com.milhub.product_service.repository;

import com.milhub.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    
    // Only join @ManyToOne category to allow pure SQL LIMIT/OFFSET pagination without in-memory warning (HHH000104).
    // imageUrls will be batch-fetched via hibernate.default_batch_fetch_size: 50.
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "imageUrls"})
    Optional<Product> findById(UUID id);

    // Batch retrieval with both associations safely fetched via single JOIN (no pagination)
    @EntityGraph(attributePaths = {"category", "imageUrls"})
    java.util.List<Product> findAllByIdIn(java.util.Collection<UUID> ids);
}