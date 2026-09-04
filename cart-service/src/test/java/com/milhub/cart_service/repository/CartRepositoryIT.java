package com.milhub.cart_service.repository;

import com.milhub.cart_service.entity.Cart;
import com.milhub.cart_service.entity.CartItem;
import com.milhub.cart_service.entity.CartSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(excludeAutoConfiguration = {
    de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration.class
})
@Testcontainers
class CartRepositoryIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private CartRepository cartRepository;

    @AfterEach
    void cleanUp() {
        cartRepository.deleteAll();
    }

    private Cart createSampleCart(UUID userId, UUID sellerId, UUID productId, int quantity, BigDecimal price) {
        Cart cart = Cart.builder()
                .userId(userId)
                .sections(new ArrayList<>())
                .totalPrice(price.multiply(BigDecimal.valueOf(quantity)))
                .build();

        CartSection section = new CartSection(sellerId, "Tactical Store", "https://minio.milhub.ua/logos/seller.png", new ArrayList<>());
        CartItem item = new CartItem(productId, "Tactical Helmet FAST", "https://minio.milhub.ua/products/helmet.png", quantity, price);
        section.getItems().add(item);
        cart.getSections().add(section);

        return cart;
    }

    @Test
    @DisplayName("Should save and retrieve cart document in real MongoDB container")
    void testSaveAndFindCart() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Cart cart = createSampleCart(userId, sellerId, productId, 2, new BigDecimal("4500.00"));
        Cart saved = cartRepository.save(cart);

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getVersion()).isNotNull();

        Optional<Cart> foundOpt = cartRepository.findById(userId);
        assertThat(foundOpt).isPresent();

        Cart found = foundOpt.get();
        assertThat(found.getSections()).hasSize(1);
        assertThat(found.getTotalPrice()).isEqualByComparingTo("9000.00");

        CartSection section = found.getSections().get(0);
        assertThat(section.getSellerId()).isEqualTo(sellerId);
        assertThat(section.getSellerName()).isEqualTo("Tactical Store");
        assertThat(section.getItems()).hasSize(1);

        CartItem item = section.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo("Tactical Helmet FAST");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getPrice()).isEqualByComparingTo("4500.00");
    }

    @Test
    @DisplayName("Should increment version upon updating cart document (MongoDB Optimistic Locking)")
    void testOptimisticLockingVersionIncrement() {
        UUID userId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Cart cart = createSampleCart(userId, sellerId, productId, 1, new BigDecimal("1000.00"));
        Cart saved = cartRepository.save(cart);
        Long initialVersion = saved.getVersion();

        // Update item quantity and total price
        saved.getSections().get(0).getItems().get(0).setQuantity(3);
        saved.setTotalPrice(new BigDecimal("3000.00"));
        Cart updated = cartRepository.save(saved);

        assertThat(updated.getVersion()).isGreaterThan(initialVersion);

        Cart reFetched = cartRepository.findById(userId).orElseThrow();
        assertThat(reFetched.getSections().get(0).getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(reFetched.getTotalPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("Should delete cart document when user checks out or clears cart")
    void testDeleteCart() {
        UUID userId = UUID.randomUUID();
        Cart cart = createSampleCart(userId, UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("500.00"));
        cartRepository.save(cart);

        assertThat(cartRepository.findById(userId)).isPresent();

        cartRepository.deleteById(userId);

        assertThat(cartRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("Should maintain data isolation between multiple user carts")
    void testMultipleUserCartsIsolation() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        Cart cart1 = createSampleCart(user1, UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("100.00"));
        Cart cart2 = createSampleCart(user2, UUID.randomUUID(), UUID.randomUUID(), 2, new BigDecimal("200.00"));
        Cart cart3 = createSampleCart(user3, UUID.randomUUID(), UUID.randomUUID(), 3, new BigDecimal("300.00"));

        cartRepository.saveAll(List.of(cart1, cart2, cart3));

        assertThat(cartRepository.count()).isEqualTo(3);
        assertThat(cartRepository.findById(user1).orElseThrow().getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(cartRepository.findById(user2).orElseThrow().getTotalPrice()).isEqualByComparingTo("400.00");
        assertThat(cartRepository.findById(user3).orElseThrow().getTotalPrice()).isEqualByComparingTo("900.00");
    }
}
