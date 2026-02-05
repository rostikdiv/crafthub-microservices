package com.crafthub.order_service.repository;

import com.crafthub.order_service.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, UUID> {
    List<OrderReturn> findByOrderId(UUID orderId);
}
