package com.milhub.product_service.controller;

import com.milhub.product_service.dto.product.ProductResponseDTO;
import com.milhub.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void getAllProducts_ShouldReturnPaginatedProducts() throws Exception {
        // Arrange
        ProductResponseDTO productDTO = new ProductResponseDTO(
                UUID.randomUUID(), "Test Product", "Description", BigDecimal.valueOf(100),
                null, 10, "Electronics", "PUBLIC", UUID.randomUUID(), "Seller", null, 4.5, 10,
                1.0, 1.0, 1.0, 1.0, "image.jpg", List.of()
        );
        Page<ProductResponseDTO> page = new PageImpl<>(List.of(productDTO));

        when(productService.getAllProducts(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.content[0].price").value(100));
    }
}
