package com.milhub.cart_service.service;

import com.milhub.cart_service.dto.CartItemRequestDTO;
import com.milhub.cart_service.dto.ProductResponseDTO;
import com.milhub.cart_service.entity.Cart;
import com.milhub.cart_service.entity.CartItem;
import com.milhub.cart_service.entity.CartSection;
import com.milhub.cart_service.repository.CartRepository;
import com.milhub.cart_service.security.UserContextService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CartServiceIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @MockBean
    private ProductServiceIntegration productServiceIntegration;

    @MockBean
    private UserContextService userContextService;

    private UUID userId;
    private UUID productId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        cartRepository.deleteAll();

        // Initial cart setup
        Cart cart = Cart.builder()
                .userId(userId)
                .sections(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .build();

        CartSection section = new CartSection(sellerId, "Test Seller", "logo.png", new ArrayList<>());
        CartItem item = new CartItem(productId, "Test Product", "img.png", 1, BigDecimal.TEN);
        section.getItems().add(item);
        cart.getSections().add(section);

        cartRepository.save(cart); // Saves version 0
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
    }

    @Test
    void testConcurrentAddItemToCart_OptimisticLocking_PreventsLostUpdates() throws InterruptedException {
        int threadCount = 5;
        int addQuantity = 2; // Each thread adds 2 to the existing item

        ProductResponseDTO mockedProduct = new ProductResponseDTO(
                productId, "Test Product", BigDecimal.TEN, 100, "img.png",
                sellerId, "Test Seller", "logo.png"
        );

        when(productServiceIntegration.getProductById(productId)).thenReturn(mockedProduct);
        when(userContextService.getUserId()).thenReturn(userId);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    CartItemRequestDTO req = new CartItemRequestDTO(productId, addQuantity);
                    cartService.addItemToCart(userId, req);
                } catch (Exception e) {
                    System.err.println("Thread failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        Cart updatedCart = cartRepository.findById(userId).orElseThrow();
        int totalQuantity = updatedCart.getSections().get(0).getItems().get(0).getQuantity();

        assertThat(totalQuantity).isEqualTo(11);
        assertThat(updatedCart.getVersion()).isGreaterThan(0L);
    }
}
