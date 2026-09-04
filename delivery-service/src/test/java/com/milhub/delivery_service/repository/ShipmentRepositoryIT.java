package com.milhub.delivery_service.repository;

import com.milhub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.milhub.delivery_service.entity.Branch;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.DeliveryStatus;
import com.milhub.delivery_service.entity.DeliveryType;
import com.milhub.delivery_service.entity.Location;
import com.milhub.delivery_service.entity.Shipment;
import com.milhub.delivery_service.entity.enums.ShipmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class ShipmentRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @DisplayName("Should save and retrieve shipment with native JSONB delivery details")
    void testSaveAndFindShipmentWithNativeJsonb() {
        UUID orderId = UUID.randomUUID();
        DeliveryDetailsDTO deliveryDetails = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA,
                DeliveryType.BRANCH,
                "e718a680-4b33-11e4-ab6d-005056801329",
                "Київ",
                "Київська",
                "16ecf56b-4b34-11e4-ab6d-005056801329",
                "Відділення №1: вул. Пирогівський шлях, 135",
                null, null, null, null,
                null, null, null
        );

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PREPARING)
                .type(ShipmentType.OUTBOUND)
                .trackingNumber("20450000000001")
                .deliveryDetails(deliveryDetails)
                .build();

        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Shipment> retrievedOpt = shipmentRepository.findByOrderId(orderId);
        assertThat(retrievedOpt).isPresent();

        Shipment retrieved = retrievedOpt.get();
        assertThat(retrieved.getTrackingNumber()).isEqualTo("20450000000001");
        assertThat(retrieved.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
        assertThat(retrieved.getType()).isEqualTo(ShipmentType.OUTBOUND);

        // Verify native PostgreSQL JSONB deserialization
        DeliveryDetailsDTO retrievedDetails = retrieved.getDeliveryDetails();
        assertThat(retrievedDetails).isNotNull();
        assertThat(retrievedDetails.provider()).isEqualTo(DeliveryProvider.NOVA_POSHTA);
        assertThat(retrievedDetails.type()).isEqualTo(DeliveryType.BRANCH);
        assertThat(retrievedDetails.cityName()).isEqualTo("Київ");
        assertThat(retrievedDetails.region()).isEqualTo("Київська");
        assertThat(retrievedDetails.branchName()).isEqualTo("Відділення №1: вул. Пирогівський шлях, 135");
    }

    @Test
    @DisplayName("Should find shipment by tracking number")
    void testFindByTrackingNumber() {
        UUID orderId = UUID.randomUUID();
        String ttn = "20459999888877";

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.SHIPPED)
                .type(ShipmentType.OUTBOUND)
                .trackingNumber(ttn)
                .build();

        shipmentRepository.saveAndFlush(shipment);

        Optional<Shipment> found = shipmentRepository.findByTrackingNumber(ttn);
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
        assertThat(found.get().getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
    }

    @Test
    @DisplayName("Should persist multiple shipments for a single order (OUTBOUND and RETURN)")
    void testMultipleShipmentsForOrder() {
        UUID orderId = UUID.randomUUID();

        Shipment outbound = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.DELIVERED)
                .type(ShipmentType.OUTBOUND)
                .trackingNumber("20450000000010")
                .build();

        Shipment returnShipment = Shipment.builder()
                .orderId(orderId)
                .status(DeliveryStatus.PREPARING)
                .type(ShipmentType.RETURN)
                .trackingNumber("20450000000011")
                .build();

        shipmentRepository.saveAllAndFlush(List.of(outbound, returnShipment));

        List<Shipment> allShipments = shipmentRepository.findAll();
        List<Shipment> orderShipments = allShipments.stream()
                .filter(s -> s.getOrderId().equals(orderId))
                .toList();

        assertThat(orderShipments).hasSize(2);
        assertThat(orderShipments).extracting(Shipment::getType)
                .containsExactlyInAnyOrder(ShipmentType.OUTBOUND, ShipmentType.RETURN);
    }

    @Test
    @DisplayName("Should manage Location and Branch entities with EntityGraph and custom queries")
    void testLocationAndBranchOperations() {
        // 1. Create Location
        Location kyivLocation = Location.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .externalId("loc-ref-kyiv-01")
                .nameUkr("Київ")
                .region("Київська")
                .build();

        Location lvivLocation = Location.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .externalId("loc-ref-lviv-01")
                .nameUkr("Львів")
                .region("Львівська")
                .build();

        locationRepository.saveAllAndFlush(List.of(kyivLocation, lvivLocation));

        // 2. Create Branches
        Branch branch1 = Branch.builder()
                .location(kyivLocation)
                .externalId("br-ref-001")
                .branchNumber("1")
                .name("Відділення №1")
                .build();

        Branch branch2 = Branch.builder()
                .location(kyivLocation)
                .externalId("br-ref-002")
                .branchNumber("2")
                .name("Відділення №2")
                .build();

        branchRepository.saveAllAndFlush(List.of(branch1, branch2));

        // 3. Test Location queries
        List<Location> searchResults = locationRepository.findByProviderAndNameUkrContainingIgnoreCase(
                DeliveryProvider.NOVA_POSHTA, "киї");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getNameUkr()).isEqualTo("Київ");

        List<String> distinctRegions = locationRepository.findDistinctRegionsByProvider(DeliveryProvider.NOVA_POSHTA);
        assertThat(distinctRegions).contains("Київська", "Львівська");

        // 4. Test Branch queries
        List<Branch> kyivBranches = branchRepository.findByLocationId(kyivLocation.getId());
        assertThat(kyivBranches).hasSize(2);
        assertThat(kyivBranches).extracting(Branch::getBranchNumber)
                .containsExactlyInAnyOrder("1", "2");

        // 5. Test Branch EntityGraph pagination
        Page<Branch> branchPage = branchRepository.findByLocationProvider(
                DeliveryProvider.NOVA_POSHTA, PageRequest.of(0, 10));
        assertThat(branchPage.getContent()).hasSize(2);
        assertThat(branchPage.getContent().get(0).getLocation().getNameUkr()).isEqualTo("Київ");
    }
}
