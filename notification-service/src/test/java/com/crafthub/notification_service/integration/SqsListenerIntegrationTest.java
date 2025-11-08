package com.crafthub.notification_service.integration;

import com.crafthub.notification_service.listeners.SqsListeners;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("aws") // Вмикаємо наш "aws" профіль
class SqsListenerIntegrationTest {

    private static final String QUEUE_NAME = "orders_queue";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0")
    ).withServices(LocalStackContainer.Service.SQS);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint",
                () -> localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region", localStack::getRegion);
    }

    @Autowired
    private SqsTemplate sqsTemplate;

    // ❗️ Інжектимо SqsAsyncClient окремо
    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @SpyBean
    private SqsListeners sqsListeners;

    @BeforeEach
    void setup() {
        // Створюємо чергу через SqsAsyncClient
        sqsAsyncClient.createQueue(builder -> builder.queueName(QUEUE_NAME))
                .join(); // .join() чекає, поки асинхронна операція завершиться
    }

    @Test
    @DisplayName("Should receive message from SQS when aws profile is active")
    void shouldReceiveMessageFromSqs() {
        // --- 1. ARRANGE ---
        String testMessage = "{\"message\":\"test-sqs-message\"}";

        // --- 2. ACT ---
        sqsTemplate.send(QUEUE_NAME, testMessage);

        // --- 3. ASSERT ---
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // Перевіряємо, що метод був викликаний
            // ❗️ Використовуємо anyString() або any() залежно від сигнатури методу
            verify(sqsListeners, times(1)).handleOrderNotification(anyString());
        });
    }
}