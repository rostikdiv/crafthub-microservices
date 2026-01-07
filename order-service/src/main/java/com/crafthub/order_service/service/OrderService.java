package com.crafthub.order_service.service;

import com.crafthub.order_service.client.ProductServiceClient;
import com.crafthub.order_service.dto.OrderItemRequestDTO;
import com.crafthub.order_service.dto.OrderRequestDTO;
import com.crafthub.order_service.dto.event.OrderPlacedEventDTO;
import com.crafthub.order_service.dto.external.ProductResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.exception.AccessDeniedException;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.security.JwtParserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final JwtParserService jwtParserService;

    // Паблішери подій (Kafka/SQS)
    @Autowired(required = false) private KafkaPublisherService kafkaPublisherService;
    @Autowired(required = false) private SqsPublisherService sqsPublisherService;

    @Transactional
    public String createOrder(OrderRequestDTO request) {
        // 1. Отримуємо токен та дані користувача
        String token = getTokenFromRequest();
        UUID userId = jwtParserService.extractUserId(token);
        String userRole = jwtParserService.extractUserRole(token);

        log.info("Creating order for User: {}, Role: {}", userId, userRole);

        // 2. Базова перевірка прав
        if (!"BUYER".equals(userRole) && !"MILITARY_UNIT".equals(userRole)) {
            // Можна дозволити і ADIMN-у, залежно від бізнес-логіки
            throw new AccessDeniedException("Only buyers or Military Units can place orders.");
        }

        // Створюємо заготовку замовлення
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<String> productNames = new ArrayList<>();

        // 3. Перевірка цін та наявності (Iterate through items)
        for (OrderItemRequestDTO itemRequest : request.items()) {

            // А. Робимо запит до Product Service (Отримуємо АКТУАЛЬНІ дані)
            ProductResponseDTO product = productServiceClient.getProductById(itemRequest.productId());

            // Б. Перевірка доступу до специфічного товару
            if ("RESTRICTED".equals(product.accessLevel()) && !"MILITARY_UNIT".equals(userRole)) {
                throw new AccessDeniedException("Access Denied: Product " + product.name() + " requires Military verification.");
            }

            // В. Перевірка залишків (Тут краще мати окремий метод у ProductService 'reduceStock', але поки читаємо)
            if (product.quantity() < itemRequest.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.name());
            }

            // Г. Розрахунок ціни (Беремо ціну з бази, а не з DTO користувача!)
            BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalOrderPrice = totalOrderPrice.add(itemTotal);
            productNames.add(product.name());

            // Д. Створюємо OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .pricePerUnit(product.price()) // Зберігаємо історичну ціну
                    .order(order) // Прив'язуємо до батьківського order
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalPrice(totalOrderPrice);

        // 4. Зберігаємо (Cascade збереже і Items)
        orderRepository.save(order);
        log.info("✅ Order created with ID: {}", order.getId());

        // 5. Відправка повідомлення (Notification)
        sendNotification(order, productNames);

        return order.getId().toString();
    }

    private void sendNotification(Order order, List<String> productNames) {
        String summary = String.join(", ", productNames);
        // Тут ми можемо передати email, якщо дістанемо його з токена або з User Service
        OrderPlacedEventDTO event = new OrderPlacedEventDTO(
                order.getId(), order.getUserId(), "user@email.placeholder", summary, order.getTotalPrice()
        );

        if (kafkaPublisherService != null) kafkaPublisherService.sendOrderPlacedEvent(event);
        else if (sqsPublisherService != null) sqsPublisherService.sendOrderToQueue(event);
    }

    private String getTokenFromRequest() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String authHeader = requestAttributes.getRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return authHeader;
    }
}