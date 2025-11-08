package com.crafthub.order_service.integration;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.ProductResponseDTO;
import com.crafthub.order_service.model.Order;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.cloud.discovery.enabled=false")
@Testcontainers
@ActiveProfiles("aws") // ❗️ 1. Вмикаємо "aws" профіль!
class OrderServiceSqsIntegrationTest {

    private static final String QUEUE_NAME = "orders_queue";
    private String queueUrl; // URL черги, який ми отримаємо від LocalStack

    // ❗️ 2. КОНТЕЙНЕР SQS
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0")
    ).withServices(LocalStackContainer.Service.SQS);

    // ❗️ 3. КОНТЕЙНЕР POSTGRES
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16.2-alpine")
    );

    // ❗️ 4. Динамічно "підміняємо" конфігурацію Spring
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // SQS
        registry.add("spring.cloud.aws.sqs.endpoint",
                () -> localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region", localStack::getRegion);

        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Вказуємо, що ми НЕ використовуємо Kafka у цьому тесті
        registry.add("spring.kafka.bootstrap-servers", () -> "");
    }

    // ❗️ 5. Замокаємо Feign-клієнт
    @MockBean
    private ProductServiceClient productServiceClient;

    @Autowired
    private OrderService orderService; // Наш сервіс, який тестуємо

    @Autowired
    private SqsAsyncClient sqsAsyncClient; // Клієнт SQS для перевірки

    @Autowired
    private OrderRepository orderRepository; // Для очищення БД

    @BeforeEach
    void setup() {
        // Створюємо чергу SQS в LocalStack і зберігаємо її URL
        queueUrl = sqsAsyncClient.createQueue(builder -> builder.queueName(QUEUE_NAME))
                .join()
                .queueUrl();
    }

    @AfterEach
    void cleanup() {
        // Очищуємо БД після тесту
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save order and send message to SQS when aws profile is active")
    void shouldSendSqsMessageOnOrderCreated() {
        // --- 1. ARRANGE ---
        String userEmail = "test@user.com";

        // 1a. Налаштовуємо мок для Feign-клієнта (щоб він не падав)
        ProductResponseDTO fakeProduct = new ProductResponseDTO(
                1L, "Test Product", new BigDecimal("10.00"), 50
        );
        when(productServiceClient.getProductById("1")).thenReturn(fakeProduct);

        // 1b. Готуємо запит на створення замовлення
        OrderItemRequestDTO item = new OrderItemRequestDTO("1", 2);
        OrderRequestDTO orderRequest = new OrderRequestDTO(List.of(item));

        // --- 2. ACT ---
        // Викликаємо наш головний метод
        Order createdOrder = orderService.createOrder(orderRequest, userEmail);

        // --- 3. ASSERT ---

        // 3a. Перевіряємо, що замовлення збереглося в БД
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getUserId()).isEqualTo(userEmail);
        assertThat(orderRepository.count()).isEqualTo(1);

        // 3b. Асинхронно перевіряємо, чи з'явилося повідомлення в черзі SQS
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(1) // Чекати 1 секунду на повідомлення
                    .build();

            ReceiveMessageResponse response = sqsAsyncClient.receiveMessage(receiveRequest).join();

            assertThat(response.hasMessages()).isTrue();
            String messageBody = response.messages().get(0).body();

            // Перевіряємо, що в тілі повідомлення є дані нашого замовлення
            assertThat(messageBody).contains(createdOrder.getOrderNumber());
            assertThat(messageBody).contains(userEmail);
            // 10.00 * 2 = 20.00 (або 20, Jackson може обрізати нулі)
            assertThat(messageBody).contains("\"totalPrice\":20");
        });
    }
}