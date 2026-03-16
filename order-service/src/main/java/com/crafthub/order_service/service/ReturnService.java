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

/**
 * Service for managing product returns and refunds.
 */
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

        // 1. Find and validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getUserId().equals(userContextService.getUserId())) {
            throw new BusinessException("You can only return items from your own orders");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Only DELIVERED orders can be returned");
        }

        // Check 14-day return period
        if (order.getUpdatedAt().plusDays(14).isBefore(LocalDateTime.now())) {
            throw new BusinessException("Return period (14 days) has expired");
        }

        OrderItem orderItem = order.getItems().stream()
                .filter(item -> item.getProductId().toString().equals(request.orderItemId())
                        || item.getId().toString().equals(request.orderItemId())) // Flexible search
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order"));

        if (request.quantity() > orderItem.getQuantity()) {
            throw new BusinessException("Cannot return more items than purchased");
        }

        // 2. Create return shipping label
        // Weight: simplified to 1.0 kg for MVP. Ideally should be fetched from Product
        // Service.
        ReturnShipmentRequestDTO shipmentRequest = new ReturnShipmentRequestDTO(
                order.getId(),
                request.returnAddress(),
                1.0 // TODO: Get real weight from product
        );

        ReturnShipmentResponseDTO shipmentResponse = deliveryService.createReturnShipment(shipmentRequest);

        // 3. Financial calculation
        ReturnReason reason = ReturnReason.valueOf(request.reason());
        BigDecimal itemPriceSnapshot = orderItem.getPricePerUnit();
        BigDecimal totalRefundAmount = itemPriceSnapshot.multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal returnShippingCost = shipmentResponse.shippingCost();
        BigDecimal finalRefundAmount;
        boolean isShippingDeducted = false;

        if (reason == ReturnReason.CHANGED_MIND || reason == ReturnReason.DID_NOT_FIT) {
            // Customer pays for shipping (deduct from refund amount)
            finalRefundAmount = totalRefundAmount.subtract(returnShippingCost);
            isShippingDeducted = true;
            if (finalRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalRefundAmount = BigDecimal.ZERO; // Cannot deduct more than the item price
            }
        } else {
            // Defect or store error - full refund, store pays for shipping
            finalRefundAmount = totalRefundAmount;
        }

        // 4. Save return request
        OrderReturn orderReturn = OrderReturn.builder()
                .order(order)
                .orderItemId(orderItem.getId()) // Store OrderItem entity ID
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
        log.info("Return request saved: {}", orderReturn.getId());

        // 5. Update order status?
        // Usually order transitions to REFUNDING only when ALL items are returned.
        // For now, we update the status to RETURN_REQUESTED for the entire order in
        // MVP.
        order.setStatus(OrderStatus.RETURN_REQUESTED);
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

            // 1. Refund payment
            if (orderReturn.getFinalRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                paymentIntegrationService.refundPayment(orderReturn.getOrder().getId(),
                        orderReturn.getFinalRefundAmount());
            }

            orderReturn.setStatus(ReturnStatus.REFUNDED);
            log.info("💰 Refund processed for return: {}", returnId);

            // 2. Send event to restore stock
            com.crafthub.order_service.dto.event.RefundApprovedEventDTO event = new com.crafthub.order_service.dto.event.RefundApprovedEventDTO(
                    orderReturn.getOrder().getId(),
                    orderReturn.getProductId(),
                    orderReturn.getQuantity(),
                    orderReturn.getReason().name());

            if (kafkaPublisherService != null) {
                kafkaPublisherService.sendRefundApprovedEvent(event);
            }

            // 3. Update order status (simplified to REFUNDED for MVP)
            orderReturn.getOrder().setStatus(OrderStatus.REFUNDED);
            orderRepository.save(orderReturn.getOrder());

        } else {
            orderReturn.setStatus(ReturnStatus.REJECTED);
            // Revert order status back to DELIVERED
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
