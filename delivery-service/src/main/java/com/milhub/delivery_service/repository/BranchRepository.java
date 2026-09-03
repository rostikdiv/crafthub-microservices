package com.milhub.delivery_service.repository;

import com.milhub.delivery_service.entity.Branch;
import com.milhub.delivery_service.entity.DeliveryProvider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * Retrieves all branches for a specific delivery provider with the location
     * eager-fetched.
     * Safe for pagination because @ManyToOne does not produce cartesian duplicates.
     *
     * @param provider the delivery provider
     * @param pageable pagination information
     * @return a paginated list of branches
     */
    @EntityGraph(attributePaths = { "location" })
    Page<Branch> findByLocationProvider(
            DeliveryProvider provider,
            Pageable pageable);
}