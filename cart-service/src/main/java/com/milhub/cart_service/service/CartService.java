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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core service for shopping cart management.
 * Handles cart persistence, item management, and price/stock synchronization
 * with the Product Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceIntegration productServiceIntegration;
    private final UserContextService userContext;

    // =========================================================================
    // PUBLIC FACADE METHODS (For the Controller)
    // =========================================================================

    /**
     * Retrieves the cart for the current authenticated user.
     * 
     * @return The current user's cart.
     */
    public Cart getMyCart() {
        return getCart(userContext.getUserId());
    }

    /**
     * Adds an item to the current authenticated user's cart.
     * 
     * @param itemDto Details of the item to add.
     * @return The updated cart.
     */
    public Cart addItemToMyCart(CartItemRequestDTO itemDto) {
        return addItemToCart(userContext.getUserId(), itemDto);
    }

    /**
     * Removes a product from the current authenticated user's cart.
     * 
     * @param productIdStr UUID of the product as a string.
     * @return The updated cart.
     */
    public Cart removeItemFromMyCart(String productIdStr) {
        return removeItemFromCart(userContext.getUserId(), productIdStr);
    }

    /**
     * Clears all items from the current authenticated user's cart.
     */
    public void clearMyCart() {
        clearCart(userContext.getUserId());
    }

    // =========================================================================
    // INTERNAL LOGIC METHODS
    // =========================================================================

    /**
     * Retrieves and synchronizes the cart for a specific user ID.
     * Refreshes product data from the Product Service.
     *
     * @param userId The unique ID of the user.
     * @return The synchronized cart.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 10, backoff = @org.springframework.retry.annotation.Backoff(delay = 50, maxDelay = 300, random = true))
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

        // 1. Fetch fresh product data from the Product Service
        List<ProductResponseDTO> freshProducts = null;

        try {
            List<UUID> allProductIds = cart.getSections().stream()
                    .flatMap(section -> section.getItems().stream())
                    .map(CartItem::getProductId)
                    .toList();

            if (allProductIds.isEmpty())
                return cart;

            freshProducts = productServiceIntegration.getProductsByIds(allProductIds);

        } catch (Exception e) {
            log.warn("Product Service error, returning cached cart. Error: {}", e.getMessage());
            return cart;
        }

        // Handle fallback scenario if service is unavailable
        if (freshProducts == null || freshProducts.isEmpty()) {
            log.info("Product Service unavailable (fallback triggered). Returning cached cart for user {}", userId);
            cart.setDataUpToDate(false);
            return cart;
        }

        cart.setDataUpToDate(true);

        // Map fresh data for efficient lookup
        Map<UUID, ProductResponseDTO> productMap = freshProducts.stream()
                .collect(Collectors.toMap(ProductResponseDTO::id, Function.identity()));

        boolean cartChanged = false;
        Iterator<CartSection> sectionIterator = cart.getSections().iterator();

        while (sectionIterator.hasNext()) {
            CartSection section = sectionIterator.next();
            List<CartItem> validItems = new ArrayList<>();

            for (CartItem item : section.getItems()) {
                ProductResponseDTO freshData = productMap.get(item.getProductId());

                // Remove item if it no longer exists in Product Service
                if (freshData == null) {
                    cartChanged = true;
                    continue;
                }

                // Update dynamic fields (price, name, etc.)
                if (updateItemData(item, freshData)) {
                    cartChanged = true;
                }

                // Update seller context
                if (section.getSellerName() == null || !section.getSellerName().equals(freshData.sellerName())) {
                    section.setSellerName(freshData.sellerName());
                    section.setSellerLogoUrl(freshData.sellerLogoUrl());
                    cartChanged = true;
                }

                // Validate and adjust based on stock
                if (freshData.quantity() > 0) {
                    if (item.getQuantity() > freshData.quantity()) {
                        item.setQuantity(freshData.quantity());
                        cartChanged = true;
                    }
                    validItems.add(item);
                } else {
                    cartChanged = true; // Product is out of stock
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

    /**
     * Adds an item to a specific cart. Validates availability.
     *
     * @param userId  The ID of the user.
     * @param itemDto The requested item details.
     * @return The updated cart.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 10, backoff = @org.springframework.retry.annotation.Backoff(delay = 50, maxDelay = 300, random = true))
    public Cart addItemToCart(UUID userId, CartItemRequestDTO itemDto) {
        ProductResponseDTO product;
        try {
            product = productServiceIntegration.getProductById(itemDto.productId());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Product not found or service unavailable");
        }

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
        if (sellerId == null) {
            sellerId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        UUID finalSellerId = sellerId;
        CartSection targetSection = cart.getSections().stream()
                .filter(s -> s.getSellerId().equals(finalSellerId))
                .findFirst()
                .orElseGet(() -> {
                    CartSection newSection = new CartSection(
                            finalSellerId,
                            product.sellerName(),
                            product.sellerLogoUrl(),
                            new ArrayList<>());
                    cart.getSections().add(newSection);
                    return newSection;
                });

        Optional<CartItem> existingItem = targetSection.getItems().stream()
                .filter(item -> item.getProductId().equals(itemDto.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + itemDto.quantity();
            if (product.quantity() < newQuantity) {
                throw new BusinessException("Not enough stock. Available: " + product.quantity());
            }
            item.setQuantity(newQuantity);
            item.setPrice(product.price());
        } else {
            CartItem newItem = new CartItem(
                    itemDto.productId(),
                    product.name(),
                    product.previewImageUrl(),
                    itemDto.quantity(),
                    product.price());
            targetSection.getItems().add(newItem);
        }

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    /**
     * Removes an item from the cart.
     *
     * @param userId       The ID of the user.
     * @param productIdStr Product UUID string.
     * @return The updated cart.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 10, backoff = @org.springframework.retry.annotation.Backoff(delay = 50, maxDelay = 300, random = true))
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
                if (section.getItems().isEmpty()) {
                    sectionIterator.remove();
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

    /**
     * Deletes the entire cart for a user.
     *
     * @param userId The user's unique ID.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 10, backoff = @org.springframework.retry.annotation.Backoff(delay = 50, maxDelay = 300, random = true))
    public void clearCart(UUID userId) {
        if (cartRepository.existsById(userId)) {
            cartRepository.deleteById(userId);
            log.info("🗑️ Cart cleared for user: {}", userId);
        }
    }

    // --- HELPER METHODS ---

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