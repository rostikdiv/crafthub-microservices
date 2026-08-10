package com.milhub.delivery_service.repository;

import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    /**
     * Finds cities by name and provider for autocomplete functionality.
     *
     * @param provider the delivery provider
     * @param name     search term for city name
     * @return a list of matching locations
     */
    List<Location> findByProviderAndNameUkrContainingIgnoreCase(DeliveryProvider provider, String name);

    List<Location> findByProviderAndRegionAndNameUkrContainingIgnoreCase(DeliveryProvider provider, String region,
            String name);

    Optional<Location> findByProviderAndExternalId(DeliveryProvider provider, String externalId);

    @Query("SELECT DISTINCT l.region FROM Location l WHERE l.provider = :provider ORDER BY l.region")
    List<String> findDistinctRegionsByProvider(@Param("provider") DeliveryProvider provider);
}