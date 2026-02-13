package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.delivery.ReturnShipmentRequestDTO;
import com.crafthub.order_service.dto.delivery.ReturnShipmentResponseDTO;
import com.crafthub.order_service.dto.order.ReturnRequestDTO;
import com.crafthub.order_service.dto.order.ReturnResponseDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import com.crafthub.order_service.entity.OrderReturn;
import com.crafthub.order_service.entity.OrderStatus;
import com.crafthub.order_service.entity.enums.ReturnReason;
import com.crafthub.order_service.entity.enums.ReturnStatus;
import com.crafthub.order_service.exception.BusinessException;
import com.crafthub.order_service.exception.ResourceNotFoundException;
import com.crafthub.order_service.repository.OrderReturnRepository;
import com.crafthub.order_service.repository.OrderRepository;
import com.crafthub.order_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final DeliveryServiceIntegration deliveryService;
    private final UserContextService userContextService;
    private final PaymentIntegrationService paymentIntegrationService;

    @Autowired(required = false)
    private KafkaPublisherService kafkaPublisherService;

    @Transactional
    public ReturnResponseDTO requestReturn(UUID orderId, ReturnRequestDTO request) {
        // ... method content ...
        log.info("Processing return request for Order: {}, Item: {}", orderId, request.orderItemId());

        // 1. Пошук та валідація замовлення
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userContextService.getUserId())) {
            throw new BusinessException("You can only return items from your own orders");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Only DELIVERED orders can be returned");
        }

        // Перевірка 14 днів (якщо це implemented) - пропустимо для простоти або додамо:
        if (order.getUpdatedAt().plusDays(14).isBefore(LocalDateTime.now())) {
            throw new BusinessException("Return period (14 days) has expired");
        }

        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getProductId().toString().equals(request.orderItemId())
                        || item.getId().toString().equals(request.orderItemId())) // Гнучкий пошук
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order"));

        if (request.quantity() > orderItem.getQuantity()) {
            throw new BusinessException("Cannot return more items than purchased");
        }

        // 2. Створення зворотної накладної
        // Вага: беремо спрощено (0.5 кг) або треба тягнути з Product Service.
        // Для MVP передамо 1.0 кг, або додамо це в DTO.
        ReturnShipmentRequestDTO shipmentRequest = new ReturnShipmentRequestDTO(
                order.getId(),
                request.returnAddress(),
                1.0 // TODO: Get real weight from product
        );

        ReturnShipmentResponseDTO shipmentResponse = deliveryService.createReturnShipment(shipmentRequest);

        // 3. Фінансовий розрахунок
        ReturnReason reason = ReturnReason.valueOf(request.reason());
        BigDecimal itemPriceSnapshot = orderItem.getPricePerUnit();
        BigDecimal totalRefundAmount = itemPriceSnapshot.multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal returnShippingCost = shipmentResponse.shippingCost();
        BigDecimal finalRefundAmount;
        boolean isShippingDeducted = false;

        if (reason == ReturnReason.CHANGED_MIND || reason == ReturnReason.DID_NOT_FIT) {
            // Клієнт платить за доставку (віднімаємо з суми повернення)
            finalRefundAmount = totalRefundAmount.subtract(returnShippingCost);
            isShippingDeducted = true;
            if (finalRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalRefundAmount = BigDecimal.ZERO; // Не можемо зняти більше ніж коштував товар
            }
        } else {
            // Брак або помилка магазину - повне повернення, доставку оплачує магазин
            finalRefundAmount = totalRefundAmount;
        }

        // 4. Збереження заявки
        OrderReturn orderReturn = OrderReturn.builder()
                .order(order)
                .orderItemId(orderItem.getId()) // Зберігаємо ID сутності OrderItem
                .productId(orderItem.getProductId())
                .quantity(request.quantity())
                .reason(reason)
                .status(ReturnStatus.PENDING)
                .itemPriceSnapshot(itemPriceSnapshot)
                .returnShippingCost(returnShippingCost)
                .finalRefundAmount(finalRefundAmount)
                .isShippingDeducted(isShippingDeducted)
                .returnTrackingNumber(shipmentResponse.trackingNumber())
                .returnShipmentId(shipmentResponse.shipmentId())
                .build();

        orderReturnRepository.save(orderReturn);
        log.info("✅ Return request saved: {}", orderReturn.getId());

        // 5. Оновлення статусу замовлення?
        // Зазвичай замовлення переходить в REFUNDING тільки коли ВСІ товари
        // повертаються, або це окремий статус для OrderReturn.
        // Поки що залишимо статус замовлення DELIVERED, керуємось статусом OrderReturn.
        // Але в плані було "Змінити статус на REFUNDING". Давайте змінимо, якщо це
        // повернення.
        // Або краще не чіпати глобальний статус замовлення, якщо повернення часткове.
        // Для MVP змінимо статус замовлення на REFUNDING.
        order.setStatus(OrderStatus.REFUNDING); // Потрібно додати цей статус в OrderStatus.java, якщо його там немає
                                                // (ми додавали)
        orderRepository.save(order);

        return new ReturnResponseDTO(
                orderReturn.getId(),
                finalRefundAmount,
                shipmentResponse.trackingNumber(),
                returnShippingCost,
                orderReturn.getStatus().name());
    }

    @Transactional
    public ReturnResponseDTO approveReturn(UUID returnId, boolean approved) {
        log.info("Processing return approval for ID: {}, Approved: {}", returnId, approved);

        OrderReturn orderReturn = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        if (orderReturn.getStatus() != ReturnStatus.PENDING) {
            throw new BusinessException("Return request is already processed");
        }

        if (approved) {
            orderReturn.setStatus(ReturnStatus.APPROVED);

            // 1. Повернення коштів
            if (orderReturn.getFinalRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                paymentIntegrationService.refundPayment(orderReturn.getOrder().getId(),
                        orderReturn.getFinalRefundAmount());
            }

            orderReturn.setStatus(ReturnStatus.REFUNDED);
            log.info("💰 Refund processed for return: {}", returnId);

            // 2. Відправка події для відновлення стоку
            com.crafthub.order_service.dto.event.RefundApprovedEventDTO event = new com.crafthub.order_service.dto.event.RefundApprovedEventDTO(
                    orderReturn.getOrder().getId(),
                    orderReturn.getProductId(),
                    orderReturn.getQuantity(),
                    orderReturn.getReason().name());

            if (kafkaPublisherService != null) {
                kafkaPublisherService.sendRefundApprovedEvent(event);
            }

            // 3. Оновлення статусу замовлення (якщо всі товари повернуто - REFUNDED, інакше
            // PARTIALLY_REFUNDED? (немає статусу))
            // Для спрощення ставимо REFUNDED
            orderReturn.getOrder().setStatus(OrderStatus.REFUNDED); // Оновлюємо статус замовлення
            orderRepository.save(orderReturn.getOrder());

        } else {
            orderReturn.setStatus(ReturnStatus.REJECTED);
            // Повертаємо статус замовлення назад, якщо треба (але ми ставили REFUNDING)
            // Можна повернути DELIVERED
            orderReturn.getOrder().setStatus(OrderStatus.DELIVERED);
            orderRepository.save(orderReturn.getOrder());
        }

        orderReturnRepository.save(orderReturn);

        return new ReturnResponseDTO(
                orderReturn.getId(),
                orderReturn.getFinalRefundAmount(),
                orderReturn.getReturnTrackingNumber(),
                orderReturn.getReturnShippingCost(),
                orderReturn.getStatus().name());
    }
}
