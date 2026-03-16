package com.crafthub.delivery_service.repository;

import com.crafthub.delivery_service.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    /**
     * Retrieves all branches for a specific city.
     *
     * @param locationId the unique identifier of the city/location
     * @return a list of branches
     */
    List<Branch> findByLocationId(UUID locationId);

    /**
     * Retrieves all branches for a specific delivery provider.
     *
     * @param provider the delivery provider
     * @param pageable pagination information
     * @return a paginated list of branches
     */
    org.springframework.data.domain.Page<Branch> findByLocationProvider(
            com.crafthub.delivery_service.entity.DeliveryProvider provider,
            org.springframework.data.domain.Pageable pageable);
}