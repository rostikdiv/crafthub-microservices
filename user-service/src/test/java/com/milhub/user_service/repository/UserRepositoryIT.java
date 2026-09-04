package com.milhub.user_service.repository;

import com.milhub.user_service.entity.MilitaryProfile;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.VerificationDoc;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private MilitaryProfileRepository militaryProfileRepository;

    @Autowired
    private VerificationDocRepository verificationDocRepository;

    private User createSampleUser(String email) {
        return User.builder()
                .email(email)
                .password("hashed_password_123")
                .firstName("Taras")
                .lastName("Shevchenko")
                .phoneNumber("+380671112233")
                .role(Role.BUYER)
                .isVerified(false)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .documents(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should save user and find by unique email in PostgreSQL")
    void testFindByEmail() {
        String email = "test." + UUID.randomUUID() + "@milhub.ua";
        User user = createSampleUser(email);

        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Taras");
        assertThat(found.get().getPhoneNumber()).isEqualTo("+380671112233");
    }

    @Test
    @DisplayName("Should enforce unique constraint on email")
    void testEmailUniqueConstraint() {
        String email = "duplicate." + UUID.randomUUID() + "@milhub.ua";
        User user1 = createSampleUser(email);
        userRepository.saveAndFlush(user1);

        User user2 = createSampleUser(email);
        user2.setPhoneNumber("+380509998877");

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find user by id with eager loaded seller and military profiles")
    void testFindByIdWithProfiles() {
        String email = "seller." + UUID.randomUUID() + "@milhub.ua";
        User user = createSampleUser(email);
        user.setRole(Role.SELLER);
        User savedUser = userRepository.saveAndFlush(user);

        SellerProfile sellerProfile = SellerProfile.builder()
                .user(savedUser)
                .companyName("MilArmor UA")
                .taxId("TAX-" + UUID.randomUUID().toString().substring(0, 8))
                .rating(4.9f)
                .reviewCount(15)
                .totalSales(120)
                .autoConfirmOrders(true)
                .build();
        sellerProfileRepository.saveAndFlush(sellerProfile);

        MilitaryProfile militaryProfile = MilitaryProfile.builder()
                .user(savedUser)
                .unitNumber("A-7015")
                .edrpou("12345678")
                .commanderName("Col. Kovalenko")
                .officialAddress("Lviv Garrison")
                .build();
        militaryProfileRepository.saveAndFlush(militaryProfile);

        savedUser.setSellerProfile(sellerProfile);
        savedUser.setMilitaryProfile(militaryProfile);
        userRepository.saveAndFlush(savedUser);

        Optional<User> withProfiles = userRepository.findByIdWithProfiles(savedUser.getId());
        assertThat(withProfiles).isPresent();
        assertThat(withProfiles.get().getSellerProfile()).isNotNull();
        assertThat(withProfiles.get().getSellerProfile().getCompanyName()).isEqualTo("MilArmor UA");
        assertThat(withProfiles.get().getMilitaryProfile()).isNotNull();
        assertThat(withProfiles.get().getMilitaryProfile().getUnitNumber()).isEqualTo("A-7015");
    }

    @Test
    @DisplayName("Should find non-verified users with pending verification documents")
    void testFindUsersWithPendingDocuments() {
        String email = "unverified." + UUID.randomUUID() + "@milhub.ua";
        User user = createSampleUser(email);
        user.setIsVerified(false);
        User savedUser = userRepository.saveAndFlush(user);

        VerificationDoc doc = VerificationDoc.builder()
                .user(savedUser)
                .docUrl("https://minio.milhub.ua/docs/doc1.pdf")
                .documentType(DocumentType.MILITARY_ID)
                .status(VerificationStatus.PENDING)
                .build();
        verificationDocRepository.saveAndFlush(doc);

        savedUser.getDocuments().add(doc);
        userRepository.saveAndFlush(savedUser);

        List<User> pendingUsers = userRepository.findUsersWithPendingDocuments();
        assertThat(pendingUsers).isNotEmpty();
        assertThat(pendingUsers).extracting(User::getId).contains(savedUser.getId());
    }
}
