package com.milhub.product_service.service;

import com.milhub.product_service.dto.product.ProductRequestDTO;
import com.milhub.product_service.dto.product.ProductResponseDTO;
import com.milhub.product_service.entity.Category;
import com.milhub.product_service.entity.Product;
import com.milhub.product_service.entity.enums.AccessLevel;
import com.milhub.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;





import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")

public class ProductCacheTest {

    
    
    

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private ProductValidatorService productValidatorService;

    @Test
    void getProductById_ShouldCacheResult() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Cached Product")
                .price(BigDecimal.valueOf(50))
                .quantity(10)
                .accessLevel(AccessLevel.PUBLIC)
                .sellerId(UUID.randomUUID())
                .category(Category.builder().name("Test").build())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Ensure cache is empty initially
        cacheManager.getCache("products").evict(productId);

        // First call - should hit repository
        ProductResponseDTO firstCall = productService.getProductById(productId);
        assertNotNull(firstCall);
        verify(productRepository, times(1)).findById(productId);

        // Second call - should hit cache
        ProductResponseDTO secondCall = productService.getProductById(productId);
        assertNotNull(secondCall);
        verify(productRepository, times(1)).findById(productId); // Still 1 invocation
        
        // Assert cache contains value
        assertNotNull(cacheManager.getCache("products").get(productId));
    }

    @Test
    void updateProduct_ShouldEvictCache() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("To be updated")
                .price(BigDecimal.valueOf(50))
                .quantity(10)
                .accessLevel(AccessLevel.PUBLIC)
                .sellerId(UUID.randomUUID())
                .category(Category.builder().id(2L).name("Test").build())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Simulate item in cache
        cacheManager.getCache("products").put(productId, "dummy-data");

        // Act
        productService.updateProduct(productId, new ProductRequestDTO(
                "Updated Name", "Desc", BigDecimal.valueOf(100), 20, 2L, "PUBLIC",
                1.0, 1.0, 1.0, 1.0, "image.jpg", null
        ));

        // Assert that cache was evicted
        assertNull(cacheManager.getCache("products").get(productId));
    }
}
