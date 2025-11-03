package com.crafthub.product_service.service;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(ProductRequestDTO requestDTO) {
        log.info("Creating new product: {}", requestDTO.name());

        // Мапимо DTO на Entity
        Product product = new Product();
        product.setName(requestDTO.name());
        product.setDescription(requestDTO.description());
        product.setPrice(requestDTO.price());
        product.setStockQuantity(requestDTO.stockQuantity());

        return productRepository.save(product);
    }

    @Transactional(readOnly = true) // ❗️ Добра практика для методів, які тільки читають
    @Cacheable(value = "products", key = "#id")
    public Optional<ProductResponseDTO> getProductById(String id) {
        log.info("--- CACHE MISS --- Fetching product {} from DATABASE", id);

        // ❗️ Логіка пошуку та маппінгу тепер тут
        return productRepository.findById(Long.parseLong(id))
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStockQuantity()
                ));
    }
}