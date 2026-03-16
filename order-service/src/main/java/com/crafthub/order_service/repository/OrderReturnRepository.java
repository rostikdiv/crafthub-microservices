package com.crafthub.order_service.repository;

import com.crafthub.order_service.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing {@link OrderReturn} entities.
 */
@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, UUID> {
    /**
     * Finds all return requests associated with a specific order.
     *
     * @param orderId The ID of the order.
     * @return A list of return requests.
     */
    List<OrderReturn> findByOrderId(UUID orderId);
}
