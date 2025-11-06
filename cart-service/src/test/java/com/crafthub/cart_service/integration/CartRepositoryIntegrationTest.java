package com.crafthub.cart_service.integration;

import com.crafthub.cart_service.model.Cart;
import com.crafthub.cart_service.model.CartItem;
import com.crafthub.cart_service.repository.CartRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// ❗️ ЗАМІНА: Використовуємо @DataMongoTest замість @SpringBootTest
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
// ❗️ ЗАМІНА: Ця анотація завантажує ТІЛЬКИ шар MongoDB.
// Вона не завантажить @Service, @RestController чи Eureka.
@DataMongoTest
class CartRepositoryIntegrationTest {

    // --- 1. Оголошення Контейнера ---
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    // --- 2. Динамічна конфігурація Spring ---
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Передаємо Spring рядок підключення до *тимчасового* Mongo
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);

        registry.add("spring.data.mongodb.database", () -> "testdb");
    }

    // --- 3. Ін'єкція Репозиторію ---

    // @DataMongoTest автоматично завантажить репозиторій,
    // оскільки він є частиною "зрізу"
    @Autowired
    private CartRepository cartRepository;

    // --- 4. Логіка Очищення ---
    @AfterEach
    void cleanup() {
        cartRepository.deleteAll();
    }

    // --- 5. Тест (залишається без змін) ---

    @Test
    @DisplayName("Should save and find cart by userId")
    void shouldSaveAndFindCartByUserId() {
        // --- 1. ARRANGE ---
        String userEmail = "test@user.com";
        CartItem item = new CartItem("prod_123", 2);
        Cart cart = Cart.builder()
                .userId(userEmail) // Використовуємо email як @Id
                .items(List.of(item))
                .build();

        // --- 2. ACT (Save) ---
        Cart savedCart = cartRepository.save(cart);

        // --- 3. ASSERT (Save) ---
        assertThat(savedCart).isNotNull();
        assertThat(savedCart.getUserId()).isEqualTo(userEmail);

        // --- 4. ACT (Find) ---
        Optional<Cart> foundCart = cartRepository.findById(userEmail);

        // --- 5. ASSERT (Find) ---
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUserId()).isEqualTo(userEmail);
        assertThat(foundCart.get().getItems()).hasSize(1);
        assertThat(foundCart.get().getItems().get(0).getProductId()).isEqualTo("prod_123");
    }
}