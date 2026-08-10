package com.milhub.order_service.repository;

import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing {@link Order} entities.
 */
/**
 * Repository for managing {@link Order} entities.
 * Includes methods for finding orders by user, seller, and product.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {
    /**
     * Finds all orders placed by a specific user with pagination support.
     *
     * @param userId   The ID of the user.
     * @param pageable Pagination information.
     * @return A page of orders.
     */
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Checks if a user has purchased a specific product and if the order is in one
     * of the given statuses.
     *
     * @param userId    The ID of the user.
     * @param productId The ID of the product.
     * @param statuses  A collection of valid order statuses.
     * @return true if an order exists, false otherwise.
     */
    boolean existsByUserIdAndItemsProductIdAndStatusIn(UUID userId, UUID productId, Collection<OrderStatus> statuses);

    /**
     * Checks if a user has purchased from a specific seller and if the order is in
     * one of the given statuses.
     *
     * @param userId   The ID of the user.
     * @param sellerId The ID of the seller.
     * @param statuses A collection of valid order statuses.
     * @return true if an order exists, false otherwise.
     */
    boolean existsByUserIdAndSellerIdAndStatusIn(UUID userId, UUID sellerId, Collection<OrderStatus> statuses);

    /**
     * Finds all orders for a specific seller with pagination support.
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination information.
     * @return A page of orders.
     */
    Page<Order> findAllBySellerId(UUID sellerId, Pageable pageable);

    /**
     * Finds all orders for a specific seller with a specific status, with
     * pagination support.
     *
     * @param sellerId The ID of the seller.
     * @param status   The status of the orders to filter by.
     * @param pageable Pagination information.
     * @return A page of orders.
     */
    Page<Order> findAllBySellerIdAndStatus(UUID sellerId, OrderStatus status, Pageable pageable);
}