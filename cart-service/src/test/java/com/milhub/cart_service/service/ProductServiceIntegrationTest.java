package com.milhub.cart_service.service;

import com.milhub.cart_service.client.ProductServiceClient;
import com.milhub.cart_service.dto.ProductResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceIntegration Unit Tests")
class ProductServiceIntegrationTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private ProductServiceIntegration integration;

    @Test
    void testGetProductById() {
        UUID productId = UUID.randomUUID();
        ProductResponseDTO product = new ProductResponseDTO(
                productId, "Item", BigDecimal.TEN, 5,
                "img", UUID.randomUUID(), "Seller", "logo"
        );

        when(productServiceClient.getProductById(productId)).thenReturn(product);

        ProductResponseDTO result = integration.getProductById(productId);

        assertNotNull(result);
        assertEquals(productId, result.id());
        verify(productServiceClient, times(1)).getProductById(productId);
    }

    @Test
    void testGetProductsByIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> ids = List.of(id1, id2);

        ProductResponseDTO p1 = new ProductResponseDTO(id1, "Item1", BigDecimal.ONE, 1, "img1", null, null, null);
        ProductResponseDTO p2 = new ProductResponseDTO(id2, "Item2", BigDecimal.TEN, 2, "img2", null, null, null);

        when(productServiceClient.getProductsByIds(ids)).thenReturn(List.of(p1, p2));

        List<ProductResponseDTO> result = integration.getProductsByIds(ids);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productServiceClient, times(1)).getProductsByIds(ids);
    }

    @Test
    void testGetProductFallback() {
        UUID productId = UUID.randomUUID();
        ProductResponseDTO result = integration.getProductFallback(productId, new RuntimeException("Service down"));
        assertNull(result);
    }

    @Test
    void testGetProductsByIdsFallback() {
        UUID productId = UUID.randomUUID();
        List<ProductResponseDTO> result = integration.getProductsByIdsFallback(List.of(productId), new RuntimeException("Service down"));
        assertNull(result);
    }
}
