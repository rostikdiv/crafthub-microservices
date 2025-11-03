package com.crafthub.cart_service.service;

import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.model.Cart;
import com.crafthub.cart_service.model.CartItem;
import com.crafthub.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;

    /**
     * Отримує кошик за ID користувача (email).
     * Якщо кошик не знайдено, створює новий порожній кошик.
     */
    public Cart getCart(String userEmail) {
        log.info("Fetching cart for user: {}", userEmail);
        return cartRepository.findById(userEmail)
                .orElseGet(() -> {
                    log.info("No cart found. Creating new cart for user: {}", userEmail);
                    Cart newCart = Cart.builder().userId(userEmail).build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Додає товар у кошик.
     * Якщо товар вже є, оновлює його кількість.
     */
    public Cart addItemToCart(String userEmail, CartItemRequestDTO itemDto) {
        log.info("Adding item {} (qty: {}) to cart for user: {}",
                itemDto.productId(), itemDto.quantity(), userEmail);

        Cart cart = getCart(userEmail); // Отримати або створити кошик

        // Перевіряємо, чи товар вже є в кошику
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Товар є - оновлюємо кількість
            existingItem.get().setQuantity(itemDto.quantity());
        } else {
            // Товару немає - додаємо новий
            cart.getItems().add(new CartItem(itemDto.productId(), itemDto.quantity()));
        }

        return cartRepository.save(cart); // Зберігаємо оновлений документ
    }

    /**
     * Видаляє товар з кошика.
     */
    public Cart removeItemFromCart(String userEmail, String productId) {
        log.info("Removing item {} from cart for user: {}", productId, userEmail);

        Cart cart = getCart(userEmail);

        // Видаляємо товар зі списку, якщо він там є
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        return cartRepository.save(cart);
    }
}