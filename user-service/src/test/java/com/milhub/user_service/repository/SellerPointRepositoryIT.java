package com.milhub.user_service.repository;

import com.milhub.user_service.entity.SellerPoint;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
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
class SellerPointRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SellerPointRepository sellerPointRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save multiple seller pickup points and find all by seller profile ID")
    void testSaveAndFindAllBySellerProfileId() {
        User user = User.builder()
                .email("seller.points." + UUID.randomUUID() + "@milhub.ua")
                .password("secret123")
                .firstName("Oleg")
                .lastName("Koval")
                .phoneNumber("+380991234567")
                .role(Role.SELLER)
                .isVerified(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .build();
        userRepository.saveAndFlush(user);

        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .companyName("Koval Gear")
                .taxId("TAX-" + UUID.randomUUID().toString().substring(0, 8))
                .rating(5.0f)
                .reviewCount(1)
                .totalSales(10)
                .autoConfirmOrders(true)
                .build();
        sellerProfileRepository.saveAndFlush(profile);

        SellerPoint point1 = SellerPoint.builder()
                .sellerProfile(profile)
                .name("Kyiv Main Hub")
                .cityRef("city-kyiv")
                .cityName("Kyiv")
                .streetName("Khreshchatyk")
                .building("22")
                .phone("+380441234567")
                .build();

        SellerPoint point2 = SellerPoint.builder()
                .sellerProfile(profile)
                .name("Lviv Hub")
                .cityRef("city-lviv")
                .cityName("Lviv")
                .streetName("Horodotska")
                .building("100")
                .phone("+380321234567")
                .build();

        sellerPointRepository.saveAndFlush(point1);
        sellerPointRepository.saveAndFlush(point2);

        List<SellerPoint> points = sellerPointRepository.findAllBySellerProfileId(profile.getId());
        assertThat(points).hasSize(2);
        assertThat(points).extracting(SellerPoint::getName).containsExactlyInAnyOrder("Kyiv Main Hub", "Lviv Hub");

        // Test delete
        sellerPointRepository.delete(point1);
        sellerPointRepository.flush();

        List<SellerPoint> remaining = sellerPointRepository.findAllBySellerProfileId(profile.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getName()).isEqualTo("Lviv Hub");
    }
}
