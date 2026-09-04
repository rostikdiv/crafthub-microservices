package com.milhub.cart_service.service;

import com.milhub.cart_service.dto.CartItemRequestDTO;
import com.milhub.cart_service.dto.ProductResponseDTO;
import com.milhub.cart_service.entity.Cart;
import com.milhub.cart_service.entity.CartItem;
import com.milhub.cart_service.entity.CartSection;
import com.milhub.cart_service.exception.BusinessException;
import com.milhub.cart_service.exception.ResourceNotFoundException;
import com.milhub.cart_service.repository.CartRepository;
import com.milhub.cart_service.security.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceIntegration productServiceIntegration;

    @Mock
    private UserContextService userContext;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private UUID productId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
    }

    // =========================================================================
    // FACADE TESTS
    // =========================================================================

    @Test
    void testGetMyCart() {
        when(userContext.getUserId()).thenReturn(userId);
        when(cartRepository.findById(userId)).thenReturn(Optional.empty());

        Cart cart = cartService.getMyCart();

        assertNotNull(cart);
        assertEquals(userId, cart.getUserId());
        assertTrue(cart.getSections().isEmpty());
    }

    @Test
    void testAddItemToMyCart() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 2);
        when(userContext.getUserId()).thenReturn(userId);

        ProductResponseDTO product = new ProductResponseDTO(
                productId, "Test Item", BigDecimal.valueOf(100), 10,
                "image.png", sellerId, "Seller Shop", "logo.png"
        );
        when(productServiceIntegration.getProductById(productId)).thenReturn(product);
        when(cartRepository.findById(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart updated = cartService.addItemToMyCart(dto);

        assertNotNull(updated);
        assertEquals(1, updated.getSections().size());
        assertEquals(BigDecimal.valueOf(200), updated.getTotalPrice());
    }

    @Test
    void testRemoveItemFromMyCart() {
        when(userContext.getUserId()).thenReturn(userId);

        Cart cart = Cart.builder()
                .userId(userId)
                .sections(new ArrayList<>(List.of(
                        new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(
                                new CartItem(productId, "Product", "img", 1, BigDecimal.TEN)
                        )))
                )))
                .build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.removeItemFromMyCart(productId.toString());

        assertNotNull(result);
        assertTrue(result.getSections().isEmpty());
    }

    @Test
    void testClearMyCart() {
        when(userContext.getUserId()).thenReturn(userId);
        when(cartRepository.existsById(userId)).thenReturn(true);

        cartService.clearMyCart();

        verify(cartRepository, times(1)).deleteById(userId);
    }

    // =========================================================================
    // GET CART & SYNCHRONIZATION TESTS
    // =========================================================================

    @Test
    void testGetCart_NewCart_ReturnsEmptyCart() {
        when(cartRepository.findById(userId)).thenReturn(Optional.empty());

        Cart cart = cartService.getCart(userId);

        assertNotNull(cart);
        assertEquals(userId, cart.getUserId());
        assertTrue(cart.getSections().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.getTotalPrice());
        assertTrue(cart.isDataUpToDate());
    }

    @Test
    void testGetCart_EmptySections_ReturnsDirectly() {
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>()).build();
        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCart(userId);

        assertSame(cart, result);
        verifyNoInteractions(productServiceIntegration);
    }

    @Test
    void testGetCart_SectionsWithNoItems_ReturnsDirectly() {
        CartSection emptySection = new CartSection(sellerId, "Seller", "logo", new ArrayList<>());
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(emptySection))).build();
        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCart(userId);

        assertSame(cart, result);
        verifyNoInteractions(productServiceIntegration);
    }

    @Test
    void testGetCart_ProductServiceThrowsException_ReturnsCachedCart() {
        CartItem item = new CartItem(productId, "Old Product", "img", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(productServiceIntegration.getProductsByIds(List.of(productId)))
                .thenThrow(new RuntimeException("Product Service is down"));

        Cart result = cartService.getCart(userId);

        assertSame(cart, result);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testGetCart_ProductServiceReturnsNullOrEmpty_SetsDataNotUpToDate() {
        CartItem item = new CartItem(productId, "Old Product", "img", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(Collections.emptyList());

        Cart result = cartService.getCart(userId);

        assertSame(cart, result);
        assertFalse(result.isDataUpToDate());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testGetCart_SynchronizesChanges_ItemPriceAndStockUpdated() {
        CartItem item = new CartItem(productId, "Old Name", "old_img.png", 5, BigDecimal.valueOf(50));
        CartSection section = new CartSection(sellerId, "Old Seller", "old_logo.png", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        ProductResponseDTO fresh = new ProductResponseDTO(
                productId, "New Name", BigDecimal.valueOf(60), 3, // quantity reduced to 3
                "new_img.png", sellerId, "New Seller", "new_logo.png"
        );
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(List.of(fresh));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.getCart(userId);

        assertTrue(result.isDataUpToDate());
        assertEquals("New Name", item.getProductName());
        assertEquals("new_img.png", item.getProductImageUrl());
        assertEquals(BigDecimal.valueOf(60), item.getPrice());
        assertEquals(3, item.getQuantity()); // capped to stock
        assertEquals("New Seller", section.getSellerName());
        assertEquals("new_logo.png", section.getSellerLogoUrl());
        assertEquals(BigDecimal.valueOf(180), result.getTotalPrice()); // 3 * 60
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void testGetCart_SectionWithNullSellerName_UpdatesSellerContext() {
        CartItem item = new CartItem(productId, "Product", "img.png", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, null, null, new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        ProductResponseDTO fresh = new ProductResponseDTO(
                productId, "Product", BigDecimal.TEN, 10,
                "img.png", sellerId, "Seller Name", "seller_logo.png"
        );
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(List.of(fresh));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.getCart(userId);

        assertEquals("Seller Name", section.getSellerName());
        assertEquals("seller_logo.png", section.getSellerLogoUrl());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void testGetCart_ItemDeletedFromProductService_RemovesItemAndEmptySection() {
        CartItem item = new CartItem(productId, "Deleted Product", "img.png", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        // ProductService returns product with different ID
        UUID otherId = UUID.randomUUID();
        ProductResponseDTO other = new ProductResponseDTO(
                otherId, "Other", BigDecimal.ONE, 10,
                "img", sellerId, "Seller", "logo"
        );
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(List.of(other));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.getCart(userId);

        assertTrue(result.getSections().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getTotalPrice());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void testGetCart_ProductOutOfStock_ItemRemoved() {
        CartItem item = new CartItem(productId, "Out of Stock Product", "img.png", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        ProductResponseDTO fresh = new ProductResponseDTO(
                productId, "Out of Stock Product", BigDecimal.TEN, 0, // 0 quantity
                "img.png", sellerId, "Seller", "logo"
        );
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(List.of(fresh));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.getCart(userId);

        assertTrue(result.getSections().isEmpty());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void testGetCart_NoChanges_DoesNotSaveToRepository() {
        CartItem item = new CartItem(productId, "Same Product", "img.png", 2, BigDecimal.valueOf(50));
        CartSection section = new CartSection(sellerId, "Same Seller", "logo.png", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        ProductResponseDTO fresh = new ProductResponseDTO(
                productId, "Same Product", BigDecimal.valueOf(50), 10,
                "img.png", sellerId, "Same Seller", "logo.png"
        );
        when(productServiceIntegration.getProductsByIds(List.of(productId))).thenReturn(List.of(fresh));

        Cart result = cartService.getCart(userId);

        assertSame(cart, result);
        verify(cartRepository, never()).save(any());
    }

    // =========================================================================
    // ADD ITEM TESTS
    // =========================================================================

    @Test
    void testAddItemToCart_ProductNotFound_ThrowsException() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 1);
        when(productServiceIntegration.getProductById(productId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> cartService.addItemToCart(userId, dto));
    }

    @Test
    void testAddItemToCart_ProductServiceError_ThrowsException() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 1);
        when(productServiceIntegration.getProductById(productId)).thenThrow(new RuntimeException("Down"));

        assertThrows(ResourceNotFoundException.class, () -> cartService.addItemToCart(userId, dto));
    }

    @Test
    void testAddItemToCart_InsufficientInitialStock_ThrowsBusinessException() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 10);
        ProductResponseDTO product = new ProductResponseDTO(
                productId, "Product", BigDecimal.TEN, 5, // only 5 available
                "img", sellerId, "Seller", "logo"
        );
        when(productServiceIntegration.getProductById(productId)).thenReturn(product);

        BusinessException ex = assertThrows(BusinessException.class, () -> cartService.addItemToCart(userId, dto));
        assertTrue(ex.getMessage().contains("Not enough stock"));
    }

    @Test
    void testAddItemToCart_NullSellerId_FallsBackToZeroUUID() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 1);
        ProductResponseDTO product = new ProductResponseDTO(
                productId, "No Seller Product", BigDecimal.TEN, 10,
                "img", null, "No Seller", "logo"
        );
        when(productServiceIntegration.getProductById(productId)).thenReturn(product);
        when(cartRepository.findById(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.addItemToCart(userId, dto);

        assertEquals(1, result.getSections().size());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), result.getSections().get(0).getSellerId());
    }

    @Test
    void testAddItemToCart_ExistingItem_IncreaseQuantitySuccess() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 2);
        ProductResponseDTO product = new ProductResponseDTO(
                productId, "Product", BigDecimal.TEN, 10,
                "img", sellerId, "Seller", "logo"
        );
        when(productServiceIntegration.getProductById(productId)).thenReturn(product);

        CartItem existingItem = new CartItem(productId, "Product", "img", 3, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(existingItem)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.addItemToCart(userId, dto);

        assertEquals(5, existingItem.getQuantity()); // 3 + 2
        assertEquals(BigDecimal.valueOf(50), result.getTotalPrice());
    }

    @Test
    void testAddItemToCart_ExistingItem_ExceedsStock_ThrowsBusinessException() {
        CartItemRequestDTO dto = new CartItemRequestDTO(productId, 5);
        ProductResponseDTO product = new ProductResponseDTO(
                productId, "Product", BigDecimal.TEN, 6,
                "img", sellerId, "Seller", "logo"
        );
        when(productServiceIntegration.getProductById(productId)).thenReturn(product);

        CartItem existingItem = new CartItem(productId, "Product", "img", 3, BigDecimal.TEN); // 3 + 5 = 8 > 6
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(existingItem)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        assertThrows(BusinessException.class, () -> cartService.addItemToCart(userId, dto));
        verify(cartRepository, never()).save(any());
    }

    // =========================================================================
    // REMOVE & CLEAR TESTS
    // =========================================================================

    @Test
    void testRemoveItemFromCart_CartNotFound_ThrowsException() {
        when(cartRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeItemFromCart(userId, productId.toString()));
    }

    @Test
    void testRemoveItemFromCart_ItemNotFound_ReturnsCartUnmodified() {
        UUID otherProduct = UUID.randomUUID();
        CartItem item = new CartItem(otherProduct, "Product", "img", 1, BigDecimal.TEN);
        CartSection section = new CartSection(sellerId, "Seller", "logo", new ArrayList<>(List.of(item)));
        Cart cart = Cart.builder().userId(userId).sections(new ArrayList<>(List.of(section))).build();

        when(cartRepository.findById(userId)).thenReturn(Optional.of(cart));

        Cart result = cartService.removeItemFromCart(userId, productId.toString());

        assertSame(cart, result);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void testClearCart_Exists_DeletesCart() {
        when(cartRepository.existsById(userId)).thenReturn(true);

        cartService.clearCart(userId);

        verify(cartRepository, times(1)).deleteById(userId);
    }

    @Test
    void testClearCart_DoesNotExist_DoesNothing() {
        when(cartRepository.existsById(userId)).thenReturn(false);

        cartService.clearCart(userId);

        verify(cartRepository, never()).deleteById(any());
    }
}
