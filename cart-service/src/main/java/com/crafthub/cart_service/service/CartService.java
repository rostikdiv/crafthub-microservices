package com.crafthub.cart_service.service;

import com.crafthub.cart_service.client.ProductServiceClient;
import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.dto.ProductResponseDTO;
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.entity.CartItem;
import com.crafthub.cart_service.entity.CartSection;
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

    // --- ОТРИМАННЯ КОШИКА (З оновленням даних) ---
    public Cart getCart(UUID userId) {
        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .sections(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .build());

        if (cart.getSections().isEmpty()) {
            return cart;
        }

        // 1. Збираємо всі ID товарів з усіх секцій
        List<UUID> allProductIds = cart.getSections().stream()
                .flatMap(section -> section.getItems().stream())
                .map(CartItem::getProductId)
                .toList();

        if (allProductIds.isEmpty()) return cart;

        // 2. Отримуємо свіжі дані (Batch request)
        List<ProductResponseDTO> freshProducts;
        try {
            freshProducts = productServiceClient.getProductsByIds(allProductIds);
        } catch (Exception e) {
            log.error("Product Service unavailable. Returning cached cart.", e);
            return cart;
        }

        Map<UUID, ProductResponseDTO> productMap = freshProducts.stream()
                .collect(Collectors.toMap(ProductResponseDTO::id, Function.identity()));

        boolean cartChanged = false;
        Iterator<CartSection> sectionIterator = cart.getSections().iterator();

        // 3. Проходимо по секціях
        while (sectionIterator.hasNext()) {
            CartSection section = sectionIterator.next();
            List<CartItem> validItems = new ArrayList<>();

            for (CartItem item : section.getItems()) {
                ProductResponseDTO freshData = productMap.get(item.getProductId());

                if (freshData == null) {
                    cartChanged = true; // Товар видалено
                    continue;
                }

                // Оновлюємо дані товару
                if (updateItemData(item, freshData)) {
                    cartChanged = true;
                }

                // Оновлюємо дані секції (назва магазину/лого), якщо змінились
                // Беремо дані з першого доступного товару секції
                if (section.getSellerName() == null || !section.getSellerName().equals(freshData.sellerName())) {
                    section.setSellerName(freshData.sellerName());
                    section.setSellerLogoUrl(freshData.sellerLogoUrl());
                    cartChanged = true;
                }

                if (freshData.quantity() > 0) {
                    validItems.add(item);
                } else {
                    cartChanged = true; // Товар закінчився
                }
            }

            section.setItems(validItems);

            if (section.getItems().isEmpty()) {
                sectionIterator.remove(); // Видаляємо порожню секцію
                cartChanged = true;
            }
        }

        recalculateTotal(cart);

        if (cartChanged) {
            return cartRepository.save(cart);
        }
        return cart;
    }

    // --- ДОДАВАННЯ ТОВАРУ ---
    public Cart addItemToCart(UUID userId, CartItemRequestDTO itemDto) {
        // 1. Отримуємо товар (тут вже будуть sellerName і logo!)
        ProductResponseDTO product;
        try {
            product = productServiceClient.getProductById(itemDto.productId());
        } catch (Exception e) {
            throw new RuntimeException("Product not found or service unavailable");
        }

        if (product.quantity() < itemDto.quantity()) {
            throw new RuntimeException("Not enough stock. Available: " + product.quantity());
        }

        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .sections(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .build());

        UUID sellerId = product.sellerId();
        if (sellerId == null) {
            // Фолбек, якщо старий товар без продавця
            sellerId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        // 2. Шукаємо або створюємо секцію
        UUID finalSellerId = sellerId;
        CartSection targetSection = cart.getSections().stream()
                .filter(s -> s.getSellerId().equals(finalSellerId))
                .findFirst()
                .orElseGet(() -> {
                    // ✅ Створюємо секцію з Гарною назвою та Лого
                    CartSection newSection = new CartSection(
                            finalSellerId,
                            product.sellerName(),     // Беремо з товару
                            product.sellerLogoUrl(),  // Беремо з товару
                            new ArrayList<>()
                    );
                    cart.getSections().add(newSection);
                    return newSection;
                });

        // 3. Додаємо товар у секцію
        Optional<CartItem> existingItem = targetSection.getItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
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
            targetSection.getItems().add(newItem);
        }

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    // --- ВИДАЛЕННЯ ТОВАРУ ---
    public Cart removeItemFromCart(UUID userId, String productIdStr) {
        UUID productId = UUID.fromString(productIdStr);
        Cart cart = cartRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        boolean cartChanged = false;
        Iterator<CartSection> sectionIterator = cart.getSections().iterator();

        while (sectionIterator.hasNext()) {
            CartSection section = sectionIterator.next();
            boolean removed = section.getItems().removeIf(item -> item.getProductId().equals(productId));

            if (removed) {
                cartChanged = true;
                if (section.getItems().isEmpty()) {
                    sectionIterator.remove(); // Видаляємо секцію, якщо порожня
                }
                break;
            }
        }

        if (cartChanged) {
            recalculateTotal(cart);
            return cartRepository.save(cart);
        }
        return cart;
    }

    // --- ОЧИЩЕННЯ (Повне) ---
    public void clearCart(UUID userId) {
        if (cartRepository.existsById(userId)) {
            cartRepository.deleteById(userId);
            log.info("🗑️ Cart cleared for user: {}", userId);
        }
    }

    // --- ДОПОМІЖНІ ---
    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getSections().stream()
                .flatMap(section -> section.getItems().stream())
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
    }

    private boolean updateItemData(CartItem item, ProductResponseDTO freshData) {
        boolean changed = false;
        if (item.getPrice().compareTo(freshData.price()) != 0) {
            item.setPrice(freshData.price());
            changed = true;
        }
        if (!Objects.equals(item.getProductName(), freshData.name())) {
            item.setProductName(freshData.name());
            changed = true;
        }
        if (!Objects.equals(item.getProductImageUrl(), freshData.previewImageUrl())) {
            item.setProductImageUrl(freshData.previewImageUrl());
            changed = true;
        }
        if (item.getQuantity() > freshData.quantity()) {
            item.setQuantity(freshData.quantity());
            changed = true;
        }
        return changed;
    }
}