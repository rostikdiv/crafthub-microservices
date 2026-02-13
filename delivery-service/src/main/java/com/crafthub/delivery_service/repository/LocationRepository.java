package com.crafthub.delivery_service.repository;

import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    // Знайти місто за назвою та провайдером (для Autocomplete)
    List<Location> findByProviderAndNameUkrContainingIgnoreCase(DeliveryProvider provider, String name);

    List<Location> findByProviderAndRegionAndNameUkrContainingIgnoreCase(DeliveryProvider provider, String region,
            String name);

    Optional<Location> findByProviderAndExternalId(DeliveryProvider provider, String externalId);

    @Query("SELECT DISTINCT l.region FROM Location l WHERE l.provider = :provider ORDER BY l.region")
    List<String> findDistinctRegionsByProvider(@Param("provider") DeliveryProvider provider);
}