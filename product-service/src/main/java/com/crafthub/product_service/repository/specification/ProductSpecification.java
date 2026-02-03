package com.crafthub.product_service.repository.specification; // 👈 Новий пакет

import com.crafthub.product_service.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class ProductSpecification {

    // Прихований конструктор, бо це утилітний клас
    private ProductSpecification() {}

    public static Specification<Product> filterProducts(
            Long categoryId, // Або UUID, перевірте тип ID у вашій Entity Category
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Boolean isAvailable
    ) {
        // ✅ ВИПРАВЛЕННЯ: Починаємо з "порожньої" true-умови замість where(null)
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        // 1. Фільтр за категорією
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        // 2. Ціна ВІД
        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        // 3. Ціна ДО
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        // 4. Наявність (quantity > 0)
        if (isAvailable != null && isAvailable) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThan(root.get("quantity"), 0));
        }

        // 5. Пошук по назві (LIKE %search%)
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> {
                String likePattern = "%" + search.toLowerCase() + "%";
                return cb.like(cb.lower(root.get("name")), likePattern);
            });
        }

        return spec;
    }
}