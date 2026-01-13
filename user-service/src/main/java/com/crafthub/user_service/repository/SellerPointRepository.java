package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.SellerPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SellerPointRepository extends JpaRepository<SellerPoint, UUID> {
    List<SellerPoint> findAllBySellerProfileId(UUID sellerProfileId);
}