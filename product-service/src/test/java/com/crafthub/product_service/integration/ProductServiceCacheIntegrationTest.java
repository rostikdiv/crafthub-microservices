package com.crafthub.product_service.integration;

import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// --- 1. Анотації для Тесту ---

@Testcontainers // ❗️ Вмикає Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE) // ❗️ Запускає повний Spring Boot, але без web-сервера
@Slf4j
class ProductServiceCacheIntegrationTest {

    // --- 2. Оголошення Контейнерів ---

    @Container // ❗️ Керує життєвим циклом контейнера
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2-alpine"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379); // ❗️ Відкриваємо порт Redis

    // --- 3. Динамічна конфігурація Spring ---

    // Цей метод запускається ДО запуску Spring Boot
    // і підміняє конфігурацію з application.yml
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Передаємо Spring URL, логін і пароль від *тимчасового* PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Передаємо Spring хост і порт від *тимчасового* Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // --- 4. Ін'єкція Справжніх Бінів ---

    @Autowired
    private ProductService productService;

    // Ми використовуємо @SpyBean, а не @MockBean.
    // @SpyBean - це "шпигун": він викликає РЕАЛЬНИЙ репозиторій,
    // але дозволяє нам *підглядати* за тим, як його викликають.
    @SpyBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager; // Для очищення кешу

    // --- 5. Логіка Очищення ---

    @BeforeEach // Очищуємо кеш перед кожним тестом
    @AfterEach  // Очищуємо кеш після кожного тесту
    void clearCache() {
        // Очищуємо кеш "products"
        cacheManager.getCache("products").clear();
    }

    // --- 6. Тест ---

    @Test
    @DisplayName("Should cache product on first call and fetch from cache on second call")
    void shouldCacheProductOnFirstCall() {
        // --- 1. ARRANGE ---
        // Створюємо продукт напряму в БД
        Product savedProduct = productRepository.save(
                new Product(null, "Test Cache Product", "Desc", new BigDecimal("100.00"), 10)
        );
        String productId = savedProduct.getId().toString();

        // --- 2. ACT (Перший виклик - Cache Miss) ---
        log.info("--- TEST: First call (Cache Miss) ---");
        Optional<ProductResponseDTO> response1 = productService.getProductById(productId);

        // --- 3. ASSERT (Перший виклик) ---
        assertThat(response1).isPresent();
        assertThat(response1.get().name()).isEqualTo("Test Cache Product");
        // Перевіряємо, що "шпигун" бачив 1 виклик до БД
        verify(productRepository, times(1)).findById(savedProduct.getId());

        // --- 4. ACT (Другий виклик - Cache Hit) ---
        log.info("--- TEST: Second call (Cache Hit) ---");
        Optional<ProductResponseDTO> response2 = productService.getProductById(productId);

        // --- 5. ASSERT (Другий виклик) ---
        assertThat(response2).isPresent();
        assertThat(response2.get().name()).isEqualTo("Test Cache Product");

        // ❗️ КРИТИЧНА ПЕРЕВІРКА:
        // Ми перевіряємо, що метод findById був викликаний ВСЬОГО 1 раз (з минулого виклику).
        // Якби кеш не спрацював, тут було б 2.
        verify(productRepository, times(1)).findById(savedProduct.getId());
    }
}