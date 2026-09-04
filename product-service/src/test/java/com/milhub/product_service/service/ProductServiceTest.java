package com.milhub.product_service.service;

import com.milhub.product_service.dto.SellerInfoDTO;
import com.milhub.product_service.dto.product.ProductRequestDTO;
import com.milhub.product_service.dto.product.ProductResponseDTO;
import com.milhub.product_service.entity.Category;
import com.milhub.product_service.entity.Product;
import com.milhub.product_service.entity.enums.AccessLevel;
import com.milhub.product_service.exception.BusinessException;
import com.milhub.product_service.exception.ResourceNotFoundException;
import com.milhub.product_service.repository.CategoryRepository;
import com.milhub.product_service.repository.ProductRepository;
import com.milhub.product_service.security.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserContextService userContext;

    @Mock
    private ProductValidatorService productValidatorService;

    @InjectMocks
    private ProductService productService;

    private UUID userId;
    private UUID productId;
    private Category category;
    private Product product;
    private ProductRequestDTO requestDTO;
    private SellerInfoDTO sellerInfo;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        category = Category.builder()
                .id(1L)
                .name("Tactical Gear")
                .description("Gear category")
                .build();

        product = Product.builder()
                .id(productId)
                .name("Tactical Vest")
                .description("Bulletproof vest")
                .price(BigDecimal.valueOf(150.00))
                .oldPrice(null)
                .quantity(20)
                .category(category)
                .accessLevel(AccessLevel.PUBLIC)
                .sellerId(userId)
                .sellerName("MilCorp")
                .sellerLogoUrl("http://logo.png")
                .weight(2.5)
                .length(40.0)
                .width(30.0)
                .height(10.0)
                .previewImageUrl("http://preview.png")
                .imageUrls(new ArrayList<>(List.of("http://img1.png")))
                .averageRating(4.5)
                .reviewCount(5)
                .build();

        requestDTO = new ProductRequestDTO(
                "Tactical Vest",
                "Bulletproof vest",
                BigDecimal.valueOf(150.00),
                20,
                1L,
                "PUBLIC",
                2.5,
                40.0,
                30.0,
                10.0,
                "http://preview.png",
                List.of("http://img1.png")
        );

        sellerInfo = new SellerInfoDTO(userId, "MilCorp", "http://logo.png", true);
    }

    @Test
    @DisplayName("createProduct: successfully creates and returns product when seller is verified")
    void createProduct_WhenSellerVerified_ShouldSaveAndReturnResponse() {
        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(sellerInfo);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(productId);
            return p;
        });

        ProductResponseDTO response = productService.createProduct(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("Tactical Vest");
        assertThat(response.sellerName()).isEqualTo("MilCorp");
        assertThat(response.accessLevel()).isEqualTo("PUBLIC");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getSellerId()).isEqualTo(userId);
        assertThat(saved.getSellerName()).isEqualTo("MilCorp");
    }

    @Test
    @DisplayName("createProduct: throws AccessDeniedException when seller is not verified")
    void createProduct_WhenSellerNotVerified_ShouldThrowAccessDeniedException() {
        SellerInfoDTO unverifiedSeller = new SellerInfoDTO(userId, "MilCorp", "http://logo.png", false);
        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(unverifiedSeller);

        assertThatThrownBy(() -> productService.createProduct(requestDTO))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unverified sellers cannot create or publish products");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct: throws ResourceNotFoundException when category not found")
    void createProduct_WhenCategoryNotFound_ShouldThrowResourceNotFoundException() {
        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(sellerInfo);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found with id: 1");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct: defaults to PUBLIC when accessLevel is invalid or null")
    void createProduct_WhenAccessLevelInvalid_ShouldDefaultToPublic() {
        ProductRequestDTO invalidAccessReq = new ProductRequestDTO(
                "Tactical Vest", "Desc", BigDecimal.valueOf(100), 5, 1L, "UNKNOWN_LEVEL",
                1.0, 1.0, 1.0, 1.0, "preview.png", List.of()
        );

        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(sellerInfo);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });

        ProductResponseDTO response = productService.createProduct(invalidAccessReq);

        assertThat(response.accessLevel()).isEqualTo("PUBLIC");
    }

    @Test
    @DisplayName("createProducts: batch creation saves all products when seller is verified")
    void createProducts_WhenSellerVerified_ShouldSaveAll() {
        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(sellerInfo);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        List<ProductResponseDTO> responses = productService.createProducts(List.of(requestDTO, requestDTO));

        assertThat(responses).hasSize(2);
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    @DisplayName("createProducts: batch creation throws AccessDeniedException when unverified")
    void createProducts_WhenSellerUnverified_ShouldThrowAccessDeniedException() {
        SellerInfoDTO unverifiedSeller = new SellerInfoDTO(userId, "MilCorp", null, false);
        when(userContext.getUserId()).thenReturn(userId);
        when(productValidatorService.validateAndGetSellerInfo(userId)).thenReturn(unverifiedSeller);

        assertThatThrownBy(() -> productService.createProducts(List.of(requestDTO)))
                .isInstanceOf(AccessDeniedException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllProducts: calls repository findAll with specification and maps to DTO")
    void getAllProducts_ShouldReturnPaginatedDTOs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProductResponseDTO> result = productService.getAllProducts(
                "vest", 1L, BigDecimal.valueOf(100), BigDecimal.valueOf(200),
                true, 4.0, userId, "PUBLIC", pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(productId);
    }

    @Test
    @DisplayName("getProductById: returns response DTO when product exists")
    void getProductById_WhenFound_ShouldReturnDTO() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.getProductById(productId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("Tactical Vest");
    }

    @Test
    @DisplayName("getProductById: throws ResourceNotFoundException when product does not exist")
    void getProductById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: " + productId);
    }

    @Test
    @DisplayName("getProductsByIds: returns empty list when input is null or empty")
    void getProductsByIds_WhenNullOrEmpty_ShouldReturnEmptyList() {
        assertThat(productService.getProductsByIds(null)).isEmpty();
        assertThat(productService.getProductsByIds(List.of())).isEmpty();
        verify(productRepository, never()).findAllByIdIn(any());
    }

    @Test
    @DisplayName("getProductsByIds: returns matching DTOs when ids provided")
    void getProductsByIds_WhenIdsProvided_ShouldReturnDTOList() {
        when(productRepository.findAllByIdIn(List.of(productId))).thenReturn(List.of(product));

        List<ProductResponseDTO> results = productService.getProductsByIds(List.of(productId));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(productId);
    }

    @Test
    @DisplayName("updateProduct: throws ResourceNotFoundException when product not found")
    void updateProduct_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(productId, requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("updateProduct: successfully updates fields, dimensions, images, and category")
    void updateProduct_WhenValid_ShouldUpdateAllFields() {
        Category newCat = Category.builder().id(2L).name("Armor").build();
        ProductRequestDTO updateDTO = new ProductRequestDTO(
                "Updated Vest",
                "Updated Desc",
                BigDecimal.valueOf(180.00),
                30,
                2L,
                "RESTRICTED",
                3.0,
                45.0,
                35.0,
                15.0,
                "http://newpreview.png",
                List.of("http://new1.png", "http://new2.png")
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));

        ProductResponseDTO response = productService.updateProduct(productId, updateDTO);

        assertThat(response.name()).isEqualTo("Updated Vest");
        assertThat(response.description()).isEqualTo("Updated Desc");
        assertThat(response.price()).isEqualTo(BigDecimal.valueOf(180.00));
        assertThat(response.quantity()).isEqualTo(30);
        assertThat(response.accessLevel()).isEqualTo("RESTRICTED");
        assertThat(response.categoryName()).isEqualTo("Armor");
        assertThat(response.previewImageUrl()).isEqualTo("http://newpreview.png");
        assertThat(response.imageUrls()).containsExactly("http://new1.png", "http://new2.png");
    }

    @Test
    @DisplayName("updateProduct: throws BusinessException when access level is invalid")
    void updateProduct_WhenAccessLevelInvalid_ShouldThrowBusinessException() {
        ProductRequestDTO invalidReq = new ProductRequestDTO(
                null, null, null, null, null, "INVALID_ACCESS_LEVEL",
                null, null, null, null, null, null
        );
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProduct(productId, invalidReq))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid access level");
    }

    @Test
    @DisplayName("updateProduct: throws ResourceNotFoundException when new category does not exist")
    void updateProduct_WhenNewCategoryNotFound_ShouldThrowResourceNotFoundException() {
        ProductRequestDTO updateDTO = new ProductRequestDTO(
                null, null, null, null, 999L, null,
                null, null, null, null, null, null
        );
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(productId, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("reduceStock: reduces quantity and saves when stock is sufficient")
    void reduceStock_WhenStockSufficient_ShouldReduceAndSave() {
        product.setQuantity(10);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.reduceStock(productId, 3);

        assertThat(product.getQuantity()).isEqualTo(7);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    @DisplayName("reduceStock: throws ResponseStatusException when stock is insufficient")
    void reduceStock_WhenStockInsufficient_ShouldThrowResponseStatusException() {
        product.setQuantity(2);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reduceStock(productId, 5))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reduceStock: throws ResourceNotFoundException when product does not exist")
    void reduceStock_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.reduceStock(productId, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("restoreStock: increments quantity and saves when product exists")
    void restoreStock_WhenFound_ShouldIncrementAndSave() {
        product.setQuantity(5);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.restoreStock(productId, 4);

        assertThat(product.getQuantity()).isEqualTo(9);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    @DisplayName("restoreStock: throws ResourceNotFoundException when product does not exist")
    void restoreStock_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.restoreStock(productId, 4))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("applyDiscount: applies discount, stores oldPrice, and updates price")
    void applyDiscount_WhenValidFirstTime_ShouldSetOldPriceAndNewPrice() {
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setOldPrice(null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = productService.applyDiscount(productId, BigDecimal.valueOf(80.00));

        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(80.00));
        assertThat(product.getOldPrice()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(response.price()).isEqualTo(BigDecimal.valueOf(80.00));
        assertThat(response.oldPrice()).isEqualTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("applyDiscount: when discounted second time, keeps original oldPrice")
    void applyDiscount_WhenSubsequentDiscount_ShouldKeepOriginalOldPrice() {
        product.setPrice(BigDecimal.valueOf(80.00));
        product.setOldPrice(BigDecimal.valueOf(100.00));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.applyDiscount(productId, BigDecimal.valueOf(70.00));

        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(70.00));
        assertThat(product.getOldPrice()).isEqualTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("applyDiscount: throws BusinessException when discount price is greater than or equal to original")
    void applyDiscount_WhenDiscountHigherThanOriginal_ShouldThrowBusinessException() {
        product.setPrice(BigDecimal.valueOf(100.00));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.applyDiscount(productId, BigDecimal.valueOf(120.00)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Discount price must be lower than original price");

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("applyDiscount: throws BusinessException when discount price is zero or negative")
    void applyDiscount_WhenDiscountZeroOrNegative_ShouldThrowBusinessException() {
        product.setPrice(BigDecimal.valueOf(100.00));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.applyDiscount(productId, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Price must be greater than 0");

        assertThatThrownBy(() -> productService.applyDiscount(productId, BigDecimal.valueOf(-5.0)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("removeDiscount: restores original price and clears oldPrice")
    void removeDiscount_WhenDiscountPresent_ShouldRestoreOldPrice() {
        product.setPrice(BigDecimal.valueOf(80.00));
        product.setOldPrice(BigDecimal.valueOf(100.00));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.removeDiscount(productId);

        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(product.getOldPrice()).isNull();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    @DisplayName("removeDiscount: when no discount present, leaves product unchanged")
    void removeDiscount_WhenNoDiscountPresent_ShouldReturnProductUnchanged() {
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setOldPrice(null);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.removeDiscount(productId);

        assertThat(product.getPrice()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(product.getOldPrice()).isNull();
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deleteProduct: successfully deletes when product exists")
    void deleteProduct_WhenProductExists_ShouldDelete() {
        when(productRepository.existsById(productId)).thenReturn(true);

        productService.deleteProduct(productId);

        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("deleteProduct: throws ResourceNotFoundException when product does not exist")
    void deleteProduct_WhenProductDoesNotExist_ShouldThrowResourceNotFoundException() {
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: " + productId);

        verify(productRepository, never()).deleteById(any());
    }
}
