package com.milhub.product_service.controller;

import com.milhub.product_service.dto.product.ProductRequestDTO;
import com.milhub.product_service.dto.product.ProductResponseDTO;
import com.milhub.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerUnitTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private UUID productId;
    private ProductResponseDTO productDTO;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productDTO = new ProductResponseDTO(
                productId, "Tactical Boots", "Waterproof", BigDecimal.valueOf(120),
                null, 15, "Boots", "PUBLIC", UUID.randomUUID(), "MilStore",
                "http://logo.png", 4.7, 8, 1.2, 30.0, 20.0, 15.0,
                "http://preview.png", List.of("http://img1.png")
        );
        requestDTO = new ProductRequestDTO(
                "Tactical Boots", "Waterproof", BigDecimal.valueOf(120),
                15, 1L, "PUBLIC", 1.2, 30.0, 20.0, 15.0,
                "http://preview.png", List.of("http://img1.png")
        );
    }

    @Test
    @DisplayName("getAllProducts: returns OK with page of products")
    void getAllProducts_ShouldReturnOk() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productService.getAllProducts(any(), any(), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(productDTO)));

        ResponseEntity<Page<ProductResponseDTO>> response = productController.getAllProducts(
                "Boots", 1L, BigDecimal.TEN, BigDecimal.valueOf(200), true, 4.0, null, "PUBLIC", pageable
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("createProduct: delegates to service and returns DTO")
    void createProduct_ShouldReturnCreatedDTO() {
        when(productService.createProduct(requestDTO)).thenReturn(productDTO);

        ProductResponseDTO result = productController.createProduct(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        verify(productService).createProduct(requestDTO);
    }

    @Test
    @DisplayName("createProductsBatch: delegates to service and returns list")
    void createProductsBatch_ShouldReturnList() {
        when(productService.createProducts(List.of(requestDTO))).thenReturn(List.of(productDTO));

        List<ProductResponseDTO> result = productController.createProductsBatch(List.of(requestDTO));

        assertThat(result).hasSize(1);
        verify(productService).createProducts(List.of(requestDTO));
    }

    @Test
    @DisplayName("getProductsBatch: returns matching products for ids")
    void getProductsBatch_ShouldReturnMatchingList() {
        when(productService.getProductsByIds(List.of(productId))).thenReturn(List.of(productDTO));

        ResponseEntity<List<ProductResponseDTO>> response = productController.getProductsBatch(List.of(productId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("getProductById: returns product by UUID")
    void getProductById_ShouldReturnProduct() {
        when(productService.getProductById(productId)).thenReturn(productDTO);

        ProductResponseDTO result = productController.getProductById(productId);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Tactical Boots");
    }

    @Test
    @DisplayName("updateProduct: returns OK with updated DTO")
    void updateProduct_ShouldReturnUpdatedDTO() {
        when(productService.updateProduct(productId, requestDTO)).thenReturn(productDTO);

        ResponseEntity<ProductResponseDTO> response = productController.updateProduct(productId, requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("applyDiscount: applies discount and returns OK")
    void applyDiscount_ShouldReturnOk() {
        when(productService.applyDiscount(productId, BigDecimal.valueOf(99.00))).thenReturn(productDTO);

        ResponseEntity<ProductResponseDTO> response = productController.applyDiscount(productId, BigDecimal.valueOf(99.00));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).applyDiscount(productId, BigDecimal.valueOf(99.00));
    }

    @Test
    @DisplayName("removeDiscount: removes discount and returns OK")
    void removeDiscount_ShouldReturnOk() {
        when(productService.removeDiscount(productId)).thenReturn(productDTO);

        ResponseEntity<ProductResponseDTO> response = productController.removeDiscount(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).removeDiscount(productId);
    }

    @Test
    @DisplayName("reduceStock: reduces stock and returns OK")
    void reduceStock_ShouldReturnOk() {
        ResponseEntity<Void> response = productController.reduceStock(productId, 3, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).reduceStock(productId, 3);
    }

    @Test
    @DisplayName("restoreStock: restores stock and returns OK")
    void restoreStock_ShouldReturnOk() {
        ResponseEntity<Void> response = productController.restoreStock(productId, 5, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).restoreStock(productId, 5);
    }

    @Test
    @DisplayName("deleteProduct: deletes product and returns NoContent")
    void deleteProduct_ShouldReturnNoContent() {
        ResponseEntity<Void> response = productController.deleteProduct(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).deleteProduct(productId);
    }

    @Test
    @DisplayName("test: health check endpoint returns message")
    void test_ShouldReturnMessage() {
        ResponseEntity<String> response = productController.test();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("product service works!");
    }
}
