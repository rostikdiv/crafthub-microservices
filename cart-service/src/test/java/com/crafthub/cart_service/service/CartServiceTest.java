package com.crafthub.cart_service.service;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.model.Cart;
import com.crafthub.cart_service.model.CartItem;
import com.crafthub.cart_service.repository.CartRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private final String TEST_USER_EMAIL = "test@user.com";
    
    @Test
    @DisplayName("Should create new cart if none exists")
    void shouldCreateNewCart_IfNoneExists() {
        // --- 1. ARRANGE ---
        // "КОЛИ репозиторій шукає кошик..."
        when(cartRepository.findById(TEST_USER_EMAIL))
                .thenReturn(Optional.empty()); // "...він нічого не знаходить"

        // "КОЛИ репозиторій зберігає БУДЬ-ЯКИЙ кошик..."
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // "...повернути той самий об'єкт, що й отримав"

        // --- 2. ACT ---
        Cart result = cartService.getCart(TEST_USER_EMAIL);

        // --- 3. ASSERT ---
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_EMAIL);
        assertThat(result.getItems()).isEmpty();
        // Перевіряємо, що ми викликали 'save', щоб створити новий кошик
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should add new item to cart")
    void shouldAddNewItemToCart() {
        // --- 1. ARRANGE ---
        // Створюємо порожній кошик
        Cart emptyCart = Cart.builder().userId(TEST_USER_EMAIL).items(new ArrayList<>()).build();
        CartItemRequestDTO newItem = new CartItemRequestDTO("prod_1", 2);

        // Навчаємо моки
        when(cartRepository.findById(TEST_USER_EMAIL))
                .thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        Cart result = cartService.addItemToCart(TEST_USER_EMAIL, newItem);

        // --- 3. ASSERT ---
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod_1");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        verify(cartRepository).save(emptyCart); // Перевіряємо, що зберегли
    }

    @Test
    @DisplayName("Should update quantity of existing item in cart")
    void shouldUpdateQuantityOfExistingItem() {
        // --- 1. ARRANGE ---
        // Створюємо кошик, в якому ВЖЕ є "prod_1"
        CartItem existingItem = new CartItem("prod_1", 1);
        Cart cart = Cart.builder().userId(TEST_USER_EMAIL).items(new ArrayList<>(java.util.List.of(existingItem))).build();

        // Ми хочемо оновити "prod_1" до 5 штук
        CartItemRequestDTO updateRequest = new CartItemRequestDTO("prod_1", 5);

        // Навчаємо моки
        when(cartRepository.findById(TEST_USER_EMAIL))
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        Cart result = cartService.addItemToCart(TEST_USER_EMAIL, updateRequest);

        // --- 3. ASSERT ---
        // Розмір кошика не змінився
        assertThat(result.getItems()).hasSize(1);
        // Але кількість оновилася
        assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod_1");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("Should remove item from cart")
    void shouldRemoveItemFromCart() {
        // --- 1. ARRANGE ---
        // Створюємо кошик з двома товарами
        CartItem item1 = new CartItem("prod_1", 1);
        CartItem item2 = new CartItem("prod_2", 3);
        Cart cart = Cart.builder().userId(TEST_USER_EMAIL).items(new ArrayList<>(java.util.List.of(item1, item2))).build();

        // Навчаємо моки
        when(cartRepository.findById(TEST_USER_EMAIL))
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. ACT ---
        // Видаляємо "prod_1"
        Cart result = cartService.removeItemFromCart(TEST_USER_EMAIL, "prod_1");

        // --- 3. ASSERT ---
        assertThat(result.getItems()).hasSize(1); // Залишився 1 товар
        assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod_2"); // Залишився "prod_2"
        verify(cartRepository).save(cart);
    }
}