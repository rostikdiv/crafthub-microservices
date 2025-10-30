package com.crafthub.product_service.service;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}