package com.milhub.product_service.repository;

import com.milhub.product_service.entity.Category;
import com.milhub.product_service.entity.Product;
import com.milhub.product_service.entity.enums.AccessLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
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
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category createAndSaveCategory(String name, Category parent) {
        Category category = Category.builder()
                .name(name)
                .description(name + " description")
                .parent(parent)
                .build();
        return categoryRepository.saveAndFlush(category);
    }

    private Product createSampleProduct(String name, BigDecimal price, int quantity, AccessLevel accessLevel, Category category, UUID sellerId) {
        return Product.builder()
                .name(name)
                .description("Detailed description for " + name)
                .price(price)
                .oldPrice(price.add(new BigDecimal("50.00")))
                .quantity(quantity)
                .weight(1.5)
                .length(20.0)
                .width(15.0)
                .height(10.0)
                .previewImageUrl("https://minio.milhub.ua/products/" + UUID.randomUUID() + ".jpg")
                .imageUrls(new ArrayList<>(List.of("https://minio.milhub.ua/products/img1.jpg", "https://minio.milhub.ua/products/img2.jpg")))
                .accessLevel(accessLevel)
                .sellerId(sellerId)
                .sellerName("Tactical Store")
                .category(category)
                .averageRating(4.8)
                .reviewCount(5)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve product with category and imageUrls via EntityGraph")
    void testSaveAndFindById() {
        Category category = createAndSaveCategory("Tactical Vests", null);
        UUID sellerId = UUID.randomUUID();

        Product product = createSampleProduct("Plate Carrier Gen 3", new BigDecimal("4500.00"), 25, AccessLevel.PUBLIC, category, sellerId);
        Product saved = productRepository.saveAndFlush(product);

        Optional<Product> fetchedOpt = productRepository.findById(saved.getId());
        assertThat(fetchedOpt).isPresent();

        Product fetched = fetchedOpt.get();
        assertThat(fetched.getName()).isEqualTo("Plate Carrier Gen 3");
        assertThat(fetched.getPrice()).isEqualByComparingTo("4500.00");
        assertThat(fetched.getCategory().getName()).isEqualTo("Tactical Vests");
        assertThat(fetched.getImageUrls()).hasSize(2);
    }

    @Test
    @DisplayName("Should filter products by Specification on real PostgreSQL SQL")
    void testFilterBySpecification() {
        Category opticsCat = createAndSaveCategory("Thermal Optics", null);
        Category bootsCat = createAndSaveCategory("Tactical Boots", null);
        UUID sellerId = UUID.randomUUID();

        // 1. Restricted military thermal sight
        Product thermalSight = createSampleProduct("Thermal Scope TS-50", new BigDecimal("85000.00"), 5, AccessLevel.RESTRICTED, opticsCat, sellerId);
        // 2. Public combat boots
        Product combatBoots = createSampleProduct("Lowa Combat Boots", new BigDecimal("7200.00"), 40, AccessLevel.PUBLIC, bootsCat, sellerId);
        // 3. Public monocular
        Product monocular = createSampleProduct("Monocular 10x42", new BigDecimal("3500.00"), 15, AccessLevel.PUBLIC, opticsCat, sellerId);

        productRepository.saveAllAndFlush(List.of(thermalSight, combatBoots, monocular));

        // Filter: Category = opticsCat
        Specification<Product> inOptics = (root, query, cb) -> cb.equal(root.get("category").get("id"), opticsCat.getId());
        Page<Product> opticsPage = productRepository.findAll(inOptics, PageRequest.of(0, 10));
        assertThat(opticsPage.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Thermal Scope TS-50", "Monocular 10x42");

        // Filter: AccessLevel = RESTRICTED
        Specification<Product> restrictedOnly = (root, query, cb) -> cb.equal(root.get("accessLevel"), AccessLevel.RESTRICTED);
        Page<Product> restrictedPage = productRepository.findAll(restrictedOnly, PageRequest.of(0, 10));
        assertThat(restrictedPage.getContent()).extracting(Product::getName)
                .containsExactly("Thermal Scope TS-50");

        // Filter: Price between 3000 and 10000
        Specification<Product> priceRange = (root, query, cb) -> cb.between(root.get("price"), new BigDecimal("3000.00"), new BigDecimal("10000.00"));
        Page<Product> pricePage = productRepository.findAll(priceRange, PageRequest.of(0, 10));
        assertThat(pricePage.getContent()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Lowa Combat Boots", "Monocular 10x42");
    }

    @Test
    @DisplayName("Should batch fetch products by IDs in a single query")
    void testFindAllByIdIn() {
        Category category = createAndSaveCategory("First Aid", null);
        UUID sellerId = UUID.randomUUID();

        Product ifak = createSampleProduct("IFAK Kit", new BigDecimal("1200.00"), 100, AccessLevel.PUBLIC, category, sellerId);
        Product tourniquet = createSampleProduct("CAT Tourniquet Gen 7", new BigDecimal("850.00"), 200, AccessLevel.PUBLIC, category, sellerId);
        Product bandage = createSampleProduct("Israeli Bandage", new BigDecimal("250.00"), 500, AccessLevel.PUBLIC, category, sellerId);

        List<Product> saved = productRepository.saveAllAndFlush(List.of(ifak, tourniquet, bandage));

        List<UUID> targetIds = List.of(saved.get(0).getId(), saved.get(1).getId());
        List<Product> batchFetched = productRepository.findAllByIdIn(targetIds);

        assertThat(batchFetched).hasSize(2);
        assertThat(batchFetched).extracting(Product::getName)
                .containsExactlyInAnyOrder("IFAK Kit", "CAT Tourniquet Gen 7");
        assertThat(batchFetched.get(0).getCategory()).isNotNull();
    }

    @Test
    @DisplayName("Should atomically update and reduce product stock in PostgreSQL")
    void testStockReduction() {
        Category category = createAndSaveCategory("Helmets", null);
        UUID sellerId = UUID.randomUUID();

        Product helmet = createSampleProduct("FAST High Cut Helmet", new BigDecimal("14000.00"), 10, AccessLevel.PUBLIC, category, sellerId);
        Product saved = productRepository.saveAndFlush(helmet);

        // Deduct 3 units
        saved.setQuantity(saved.getQuantity() - 3);
        productRepository.saveAndFlush(saved);

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }
}
