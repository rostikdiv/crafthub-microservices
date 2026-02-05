package com.crafthub.cart_service.service;

import com.crafthub.cart_service.client.ProductServiceClient;
import com.crafthub.cart_service.dto.CartItemRequestDTO;
import com.crafthub.cart_service.dto.ProductResponseDTO;
import com.crafthub.cart_service.entity.Cart;
import com.crafthub.cart_service.entity.CartItem;
import com.crafthub.cart_service.entity.CartSection;
import com.crafthub.cart_service.exception.BusinessException;
import com.crafthub.cart_service.exception.ResourceNotFoundException;
import com.crafthub.cart_service.repository.CartRepository;
import com.crafthub.cart_service.security.UserContextService;
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
    private final ProductServiceIntegration productServiceIntegration;
    private final UserContextService userContext;

    // =========================================================================
    // 🟢 PUBLIC FACADE METHODS (Для Контролера)
    // Ці методи не приймають userId, а беруть його з контексту безпеки
    // =========================================================================

    public Cart getMyCart() {
        return getCart(userContext.getUserId());
    }

    public Cart addItemToMyCart(CartItemRequestDTO itemDto) {
        return addItemToCart(userContext.getUserId(), itemDto);
    }

    public Cart removeItemFromMyCart(String productIdStr) {
        return removeItemFromCart(userContext.getUserId(), productIdStr);
    }

    public void clearMyCart() {
        clearCart(userContext.getUserId());
    }

    // =========================================================================
    // 🟡 INTERNAL LOGIC METHODS
    // Реалізація бізнес-логіки з явним userId
    // =========================================================================

    // --- ОТРИМАННЯ КОШИКА (З синхронізацією цін та наявності) ---
    // --- ОТРИМАННЯ КОШИКА (З синхронізацією цін та наявності) ---
    public Cart getCart(UUID userId) {
        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .sections(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .isDataUpToDate(true)
                        .build());

        if (cart.getSections().isEmpty()) {
            return cart;
        }

        // 1. Отримуємо свіжі дані про товари з Product Service
        List<ProductResponseDTO> freshProducts = null; // Ініціалізуємо null

        try {
            List<UUID> allProductIds = cart.getSections().stream()
                    .flatMap(section -> section.getItems().stream())
                    .map(CartItem::getProductId)
                    .toList();

            if (allProductIds.isEmpty()) return cart;

            // Виклик через Circuit Breaker
            freshProducts = productServiceIntegration.getProductsByIds(allProductIds);

        } catch (Exception e) {
            log.warn("Product Service error, returning cached cart. Error: {}", e.getMessage());
            return cart;
        }

        // 🔥 ВАЖЛИВА ЗМІНА: Перевіряємо на NULL перед використанням
        // Якщо Circuit Breaker спрацював і повернув null -> ми просто віддаємо старий кошик
        if (freshProducts == null || freshProducts.isEmpty()) {
            log.info("Product Service unavailable (fallback triggered). Returning cached cart for user {}", userId);
            cart.setDataUpToDate(false);
            return cart;
        }
        // 2. Якщо ми тут — значить дані прийшли успішно, оновлюємо кошик
        cart.setDataUpToDate(true);



        // Створюємо мапу для швидкого пошуку: ID -> Product
        Map<UUID, ProductResponseDTO> productMap = freshProducts.stream()
                .collect(Collectors.toMap(ProductResponseDTO::id, Function.identity()));

        boolean cartChanged = false;
        Iterator<CartSection> sectionIterator = cart.getSections().iterator();

        while (sectionIterator.hasNext()) {
            CartSection section = sectionIterator.next();
            List<CartItem> validItems = new ArrayList<>();

            for (CartItem item : section.getItems()) {
                ProductResponseDTO freshData = productMap.get(item.getProductId());

                // Якщо товару більше не існує в базі (Product Service повернув список, але цього товару там немає)
                // Тоді видаляємо його з кошика
                if (freshData == null) {
                    cartChanged = true;
                    continue;
                }

                // Оновлюємо поля (ціна, назва, картинка)
                if (updateItemData(item, freshData)) {
                    cartChanged = true;
                }

                // Оновлюємо дані секції
                if (section.getSellerName() == null || !section.getSellerName().equals(freshData.sellerName())) {
                    section.setSellerName(freshData.sellerName());
                    section.setSellerLogoUrl(freshData.sellerLogoUrl());
                    cartChanged = true;
                }

                // Перевіряємо наявність на складі
                if (freshData.quantity() > 0) {
                    if (item.getQuantity() > freshData.quantity()) {
                        item.setQuantity(freshData.quantity());
                        cartChanged = true;
                    }
                    validItems.add(item);
                } else {
                    cartChanged = true; // Товар закінчився
                }
            }

            section.setItems(validItems);

            if (section.getItems().isEmpty()) {
                sectionIterator.remove();
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
        // 1. Отримуємо актуальні дані про товар
        ProductResponseDTO product;
        try {
            product = productServiceIntegration.getProductById(itemDto.productId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Product not found or service unavailable");
        }

        // 2. Перевіряємо залишки
        if (product.quantity() < itemDto.quantity()) {
            throw new BusinessException("Not enough stock. Available: " + product.quantity());
        }

        Cart cart = cartRepository.findById(userId)
                .orElse(Cart.builder()
                        .userId(userId)
                        .sections(new ArrayList<>())
                        .totalPrice(BigDecimal.ZERO)
                        .build());

        UUID sellerId = product.sellerId();
        // Фолбек для старих товарів без продавця
        if (sellerId == null) {
            sellerId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        // 3. Шукаємо або створюємо секцію для цього продавця
        UUID finalSellerId = sellerId;
        CartSection targetSection = cart.getSections().stream()
                .filter(s -> s.getSellerId().equals(finalSellerId))
                .findFirst()
                .orElseGet(() -> {
                    CartSection newSection = new CartSection(
                            finalSellerId,
                            product.sellerName(),
                            product.sellerLogoUrl(),
                            new ArrayList<>()
                    );
                    cart.getSections().add(newSection);
                    return newSection;
                });

        // 4. Додаємо або оновлюємо товар у секції
        Optional<CartItem> existingItem = targetSection.getItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(itemDto.quantity()); // Встановлюємо нову кількість (перезапис)
            item.setPrice(product.price());       // Оновлюємо ціну
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
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        boolean cartChanged = false;
        Iterator<CartSection> sectionIterator = cart.getSections().iterator();

        while (sectionIterator.hasNext()) {
            CartSection section = sectionIterator.next();
            boolean removed = section.getItems().removeIf(item -> item.getProductId().equals(productId));

            if (removed) {
                cartChanged = true;
                // Якщо секція стала порожньою - видаляємо її
                if (section.getItems().isEmpty()) {
                    sectionIterator.remove();
                }
                break; // Товар знайдено і видалено
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

    // --- ДОПОМІЖНІ МЕТОДИ ---

    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getSections().stream()
                .flatMap(section -> section.getItems().stream())
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
    }

    private boolean updateItemData(CartItem item, ProductResponseDTO freshData) {
        boolean changed = false;
        // Порівнюємо BigDecimal правильно (через compareTo)
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
        // Оновлюємо кількість, тільки якщо на складі стало менше, ніж у кошику
        // (Збільшення кількості користувач робить вручну)
        if (item.getQuantity() > freshData.quantity()) {
            item.setQuantity(freshData.quantity());
            changed = true;
        }
        return changed;
    }
}