package com.crafthub.cart_service.service;

import com.crafthub.cart_service.client.ProductServiceClient;
import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.dto.ProductResponseDTO; // ✅ Використовуємо правильне DTO
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.entity.CartItem;
import com.crafthub.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    public Cart getCart(UUID userId) {
        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .build());

        if (cart.getItems().isEmpty()) {
            return cart;
        }

        // 2. Batch update
        List<UUID> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .toList();

        List<ProductResponseDTO> freshProducts;
        try {
            freshProducts = productServiceClient.getProductsByIds(productIds);
        } catch (Exception e) {
            log.error("Product Service unavailable. Returning cached cart.", e);
            return cart;
        }

        Map<UUID, ProductResponseDTO> productMap = freshProducts.stream()
                .collect(Collectors.toMap(ProductResponseDTO::id, Function.identity()));

        List<CartItem> updatedItems = new ArrayList<>();
        boolean cartChanged = false;

        for (CartItem item : cart.getItems()) {
            ProductResponseDTO freshData = productMap.get(item.getProductId());

            if (freshData == null) {
                cartChanged = true; // Товар видалено з магазину
                continue;
            }

            // Оновлюємо ціну
            if (item.getPrice().compareTo(freshData.price()) != 0) {
                item.setPrice(freshData.price());
                cartChanged = true;
            }

            // Оновлюємо метадані
            if (!Objects.equals(item.getProductName(), freshData.name())) {
                item.setProductName(freshData.name());
                cartChanged = true;
            }
            if (!Objects.equals(item.getProductImageUrl(), freshData.previewImageUrl())) {
                item.setProductImageUrl(freshData.previewImageUrl());
                cartChanged = true;
            }

            // Перевірка кількості
            if (item.getQuantity() > freshData.quantity()) {
                item.setQuantity(freshData.quantity());
                cartChanged = true;
            }

            if (freshData.quantity() > 0) {
                updatedItems.add(item);
            } else {
                cartChanged = true;
            }
        }

        cart.setItems(updatedItems);
        recalculateTotal(cart); // ✅ Тепер цей метод спрацює коректно

        if (cartChanged) {
            return cartRepository.save(cart);
        }

        return cart;
    }

    public Cart addItemToCart(UUID userId, CartItemRequestDTO itemDto) {
        // 1. Запит товару
        ProductResponseDTO product;
        try {
            product = productServiceClient.getProductById(itemDto.productId());
        } catch (Exception e) {
            throw new RuntimeException("Product not found or service unavailable");
        }

        if (product.quantity() < itemDto.quantity()) {
            throw new RuntimeException("Not enough stock. Available: " + product.quantity());
        }

        // 2. Отримання кошика
        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .build());

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            // Тут ти можеш вибрати логіку: або перезаписати кількість (Set), або додати (Add)
            // Логіка перезапису (якщо фронт надсилає фінальну кількість):
            if (itemDto.quantity() > product.quantity()) {
                throw new RuntimeException("Not enough stock");
            }
            item.setQuantity(itemDto.quantity());
            item.setPrice(product.price());
        } else {
            CartItem newItem = new CartItem(
                    itemDto.productId(),
                    product.name(),
                    product.previewImageUrl(),
                    itemDto.quantity(),
                    product.price()
            );
            cart.getItems().add(newItem);
        }

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    public Cart removeItemFromCart(UUID userId, String productIdStr) {
        UUID productId = UUID.fromString(productIdStr);
        Cart cart = cartRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (removed) {
            recalculateTotal(cart);
            return cartRepository.save(cart);
        }
        return cart;
    }

    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubTotal) // Використовує метод getSubTotal з CartItem
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalPrice(total); // ✅ Тепер це поле існує в Cart
    }
}