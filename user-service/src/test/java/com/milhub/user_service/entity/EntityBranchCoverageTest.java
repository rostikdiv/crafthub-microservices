package com.milhub.user_service.entity;

import com.milhub.user_service.entity.enums.DeliveryProvider;
import com.milhub.user_service.entity.enums.DeliveryType;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.entity.enums.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityBranchCoverageTest {

    @Test
    @DisplayName("User: equals, hashCode, toString and branch coverage")
    void testUserEntityBranches() {
        UUID id = UUID.randomUUID();
        User u1 = User.builder().id(id).email("user1@milhub.ua").build();
        User u2 = User.builder().id(id).email("user1@milhub.ua").build();
        User u3 = User.builder().id(UUID.randomUUID()).build();
        User emptyId = new User();

        assertThat(u1.equals(u1)).isTrue();
        assertThat(u1.equals(null)).isFalse();
        assertThat(u1.equals("string")).isFalse();
        assertThat(u1.equals(u2)).isTrue();
        assertThat(u1.equals(u3)).isFalse();
        assertThat(emptyId.equals(u1)).isFalse();
        assertThat(u1.equals(emptyId)).isFalse();

        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        assertThat(u1.toString()).contains("user1@milhub.ua");
        assertThat(u1.isAccountNonExpired()).isTrue();
        assertThat(u1.isAccountNonLocked()).isTrue();
        assertThat(u1.isCredentialsNonExpired()).isTrue();
        assertThat(u1.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("SellerProfile: equals, hashCode, toString branches")
    void testSellerProfileBranches() {
        UUID id = UUID.randomUUID();
        SellerProfile s1 = SellerProfile.builder().id(id).companyName("Store").build();
        SellerProfile s2 = SellerProfile.builder().id(id).companyName("Store").build();
        SellerProfile s3 = SellerProfile.builder().id(UUID.randomUUID()).build();
        SellerProfile empty = new SellerProfile();

        assertThat(s1.equals(s1)).isTrue();
        assertThat(s1.equals(null)).isFalse();
        assertThat(s1.equals("string")).isFalse();
        assertThat(s1.equals(s2)).isTrue();
        assertThat(s1.equals(s3)).isFalse();
        assertThat(empty.equals(s1)).isFalse();
        assertThat(s1.equals(empty)).isFalse();

        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        assertThat(s1.toString()).contains("Store");
    }

    @Test
    @DisplayName("MilitaryProfile: equals, hashCode, toString branches")
    void testMilitaryProfileBranches() {
        UUID id = UUID.randomUUID();
        MilitaryProfile m1 = MilitaryProfile.builder().id(id).unitNumber("A1234").build();
        MilitaryProfile m2 = MilitaryProfile.builder().id(id).unitNumber("A1234").build();
        MilitaryProfile m3 = MilitaryProfile.builder().id(UUID.randomUUID()).build();
        MilitaryProfile empty = new MilitaryProfile();

        assertThat(m1.equals(m1)).isTrue();
        assertThat(m1.equals(null)).isFalse();
        assertThat(m1.equals("string")).isFalse();
        assertThat(m1.equals(m2)).isTrue();
        assertThat(m1.equals(m3)).isFalse();
        assertThat(empty.equals(m1)).isFalse();
        assertThat(m1.equals(empty)).isFalse();

        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1.toString()).contains("A1234");
    }

    @Test
    @DisplayName("SellerPoint: equals, hashCode, toString branches")
    void testSellerPointBranches() {
        UUID id = UUID.randomUUID();
        SellerPoint p1 = SellerPoint.builder().id(id).name("Point 1").build();
        SellerPoint p2 = SellerPoint.builder().id(id).name("Point 1").build();
        SellerPoint p3 = SellerPoint.builder().id(UUID.randomUUID()).build();
        SellerPoint empty = new SellerPoint();

        assertThat(p1.equals(p1)).isTrue();
        assertThat(p1.equals(null)).isFalse();
        assertThat(p1.equals("string")).isFalse();
        assertThat(p1.equals(p2)).isTrue();
        assertThat(p1.equals(p3)).isFalse();
        assertThat(empty.equals(p1)).isFalse();
        assertThat(p1.equals(empty)).isFalse();

        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1.toString()).contains("Point 1");
    }

    @Test
    @DisplayName("SavedAddress: equals, hashCode, toString branches")
    void testSavedAddressBranches() {
        UUID id = UUID.randomUUID();
        SavedAddress a1 = SavedAddress.builder().id(id).title("Home").provider(DeliveryProvider.NOVA_POSHTA).deliveryType(DeliveryType.BRANCH).build();
        SavedAddress a2 = SavedAddress.builder().id(id).title("Home").provider(DeliveryProvider.NOVA_POSHTA).deliveryType(DeliveryType.BRANCH).build();
        SavedAddress a3 = SavedAddress.builder().id(UUID.randomUUID()).build();
        SavedAddress empty = new SavedAddress();

        assertThat(a1.equals(a1)).isTrue();
        assertThat(a1.equals(null)).isFalse();
        assertThat(a1.equals("string")).isFalse();
        assertThat(a1.equals(a2)).isTrue();
        assertThat(a1.equals(a3)).isFalse();
        assertThat(empty.equals(a1)).isFalse();
        assertThat(a1.equals(empty)).isFalse();

        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.toString()).contains("Home");
    }

    @Test
    @DisplayName("VerificationDoc: equals, hashCode, toString branches")
    void testVerificationDocBranches() {
        UUID id = UUID.randomUUID();
        VerificationDoc d1 = VerificationDoc.builder().id(id).documentType(DocumentType.PASSPORT).status(VerificationStatus.PENDING).build();
        VerificationDoc d2 = VerificationDoc.builder().id(id).documentType(DocumentType.PASSPORT).status(VerificationStatus.PENDING).build();
        VerificationDoc d3 = VerificationDoc.builder().id(UUID.randomUUID()).build();
        VerificationDoc empty = new VerificationDoc();

        assertThat(d1.equals(d1)).isTrue();
        assertThat(d1.equals(null)).isFalse();
        assertThat(d1.equals("string")).isFalse();
        assertThat(d1.equals(d2)).isTrue();
        assertThat(d1.equals(d3)).isFalse();
        assertThat(empty.equals(d1)).isFalse();
        assertThat(d1.equals(empty)).isFalse();

        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
        assertThat(d1.toString()).contains("PASSPORT");
    }

    @Test
    @DisplayName("SellerReview: equals, hashCode, toString branches")
    void testSellerReviewBranches() {
        UUID id = UUID.randomUUID();
        SellerReview r1 = SellerReview.builder().id(id).rating(5).comment("Good").build();
        SellerReview r2 = SellerReview.builder().id(id).rating(5).comment("Good").build();
        SellerReview r3 = SellerReview.builder().id(UUID.randomUUID()).build();
        SellerReview empty = new SellerReview();

        assertThat(r1.equals(r1)).isTrue();
        assertThat(r1.equals(null)).isFalse();
        assertThat(r1.equals("string")).isFalse();
        assertThat(r1.equals(r2)).isTrue();
        assertThat(r1.equals(r3)).isFalse();
        assertThat(empty.equals(r1)).isFalse();
        assertThat(r1.equals(empty)).isFalse();

        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1.toString()).contains("rating=5");
    }
}
