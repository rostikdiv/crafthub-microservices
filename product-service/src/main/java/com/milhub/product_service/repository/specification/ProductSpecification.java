package com.milhub.product_service.repository.specification;

import com.milhub.product_service.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> filterProducts(
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Boolean isAvailable,
            Double minRating,
            java.util.UUID sellerId,
            String accessLevel
    ) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        // 1. Filter by category (including subcategories)
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> {
                jakarta.persistence.criteria.Path<Object> categoryPath = root.get("category");
                jakarta.persistence.criteria.Predicate isCategory = cb.equal(categoryPath.get("id"), categoryId);
                
                jakarta.persistence.criteria.Path<Object> parentPath = categoryPath.get("parent");
                jakarta.persistence.criteria.Predicate isParentCategory = cb.equal(parentPath.get("id"), categoryId);
                
                return cb.or(isCategory, isParentCategory);
            });
        }

        // 2. Minimum price
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        // 3. Maximum price
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        // 4. Availability check
        if (isAvailable != null && isAvailable) {
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("quantity"), 0));
        }

        // 5. Search by name (case-insensitive)
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> {
                String likePattern = "%" + search.toLowerCase() + "%";
                return cb.like(cb.lower(root.get("name")), likePattern);
            });
        }

        // 6. Minimum rating
        if (minRating != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
        }

        // 7. Seller ID
        if (sellerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sellerId"), sellerId));
        }

        // 8. Access Level (clearance)
        if (accessLevel != null && !accessLevel.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accessLevel"),
                    com.milhub.product_service.entity.enums.AccessLevel.valueOf(accessLevel.toUpperCase())));
        }

        return spec;
    }
}