package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.ProductResponseDTO;
import com.crafthub.order_service.model.Order;
import com.crafthub.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// ❗️ Вмикаємо розширення Mockito для JUnit 5
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // === 1. Створення "фальшивих" залежностей (Mocks) ===

    // Ми створюємо "заглушку" для репозиторію. Ми НЕ будемо
    // звертатися до реальної БД.
    @Mock
    private OrderRepository orderRepository;

    // Створюємо "заглушку" для Feign-клієнта.
    // Ми НЕ будемо робити реальний HTTP-запит.
    @Mock
    private ProductServiceClient productServiceClient;

    // Створюємо "заглушку" для Kafka.
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    // === 2. Впровадження Моків (InjectMocks) ===

    // Створюємо РЕАЛЬНИЙ екземпляр OrderService,
    // але автоматично вставляємо в нього всі наші @Mock (заглушки).
    @InjectMocks
    private OrderService orderService;


    // === 3. Наші Тестові Методи ===

    @Test
    @DisplayName("Should Create Order Successfully When Stock Is Available")
    void shouldCreateOrder_WhenStockIsAvailable() throws Exception {
        // --- 1. ARRANGE (Налаштування) ---

        // Вхідні дані: "Користувач хоче 2 товари з ID=1"
        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO("1", 2);
        OrderRequestDTO orderRequest = new OrderRequestDTO(List.of(itemRequest));
        String userEmail = "test@user.com";

        // Дані, які "поверне" наш Feign-клієнт:
        ProductResponseDTO productResponse = new ProductResponseDTO(
                1L,
                "Test Product",
                new BigDecimal("10.00"), // Ціна
                50 // На складі
        );

        // Дані, які "поверне" наш репозиторій:
        Order savedOrder = Order.builder().id(1L).orderNumber("test-uuid").build();

        // Навчаємо наші Моки:
        // "КОЛИ productServiceClient.getProductById("1") буде викликаний..."
        when(productServiceClient.getProductById("1"))
                .thenReturn(productResponse); // "...повернути наш fake productResponse"

        // "КОЛИ orderRepository.save() буде викликаний з БУДЬ-ЯКИМ об'єктом Order..."
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder); // "...повернути наш fake savedOrder"

        // "КОЛИ objectMapper.writeValueAsString() буде викликаний..."
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"json\":\"mock\"}"); // "...повернути будь-який рядок"

        // --- 2. ACT (Дія) ---

        // Викликаємо метод, який ми тестуємо
        Order result = orderService.createOrder(orderRequest, userEmail);

        // --- 3. ASSERT (Перевірка) ---

        // Перевіряємо, що результат не порожній
        assertThat(result).isNotNull();

        // Перевіряємо, що сервіс правильно розрахував суму
        // (Price 10.00 * Quantity 2 = 20.00)
        // Ми перевіряємо це через `any(Order.class)` мок
        // Більш просунутий спосіб - використовувати ArgumentCaptor,
        // але для початку перевіримо виклики.

        // Перевіряємо, що наші "заглушки" були викликані:

        // Перевірити, що Feign-клієнт був викликаний РІВНО 1 раз
        verify(productServiceClient, times(1)).getProductById("1");

        // Перевірити, що репозиторій був викликаний РІВНО 1 раз для збереження
        verify(orderRepository, times(1)).save(any(Order.class));

        // Перевірити, що KafkaTemplate був викликаний РІВНО 1 раз
        verify(kafkaTemplate, times(1)).send(eq("orders_topic"), anyString());
    }

    @Test
    @DisplayName("Should Throw Exception When Stock Is Insufficient")
    void shouldThrowException_WhenStockIsInsufficient() {
        // --- 1. ARRANGE (Налаштування) ---

        // Вхідні дані: "Користувач хоче 100 товарів з ID=1"
        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO("1", 100);
        OrderRequestDTO orderRequest = new OrderRequestDTO(List.of(itemRequest));
        String userEmail = "test@user.com";

        // Feign-клієнт "каже", що на складі є ТІЛЬКИ 50
        ProductResponseDTO productResponse = new ProductResponseDTO(
                1L,
                "Test Product",
                new BigDecimal("10.00"),
                50 // На складі
        );

        // Навчаємо Мок:
        when(productServiceClient.getProductById("1"))
                .thenReturn(productResponse);

        // (Нам не потрібно мокати 'orderRepository.save',
        // оскільки тест має впасти ДО цього)

        // --- 2. ACT & 3. ASSERT (Дія та Перевірка) ---

        // Ми очікуємо, що виклик orderService.createOrder(...)
        // призведе до винятку RuntimeException
        assertThatThrownBy(() -> {
            orderService.createOrder(orderRequest, userEmail);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock"); // Перевіряємо текст помилки

        // Перевіряємо, що в цьому випадку ми НІКОЛИ не дійшли до збереження
        verify(orderRepository, never()).save(any(Order.class));
        // І НІКОЛИ не відправили повідомлення в Kafka
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}