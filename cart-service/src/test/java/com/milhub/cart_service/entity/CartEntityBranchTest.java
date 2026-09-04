package com.milhub.cart_service.entity;

import com.milhub.cart_service.dto.CartItemRequestDTO;
import com.milhub.cart_service.dto.ErrorResponse;
import com.milhub.cart_service.dto.OrderPlacedEventDTO;
import com.milhub.cart_service.dto.ProductResponseDTO;
import com.milhub.cart_service.exception.AppException;
import com.milhub.cart_service.exception.BusinessException;
import com.milhub.cart_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart Entity & DTO Branch Coverage Tests")
class CartEntityBranchTest {

    @Test
    void testCartEqualsAndHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Cart c1 = Cart.builder().userId(id1).sections(new ArrayList<>()).totalPrice(BigDecimal.ZERO).build();
        Cart c2 = Cart.builder().userId(id1).sections(new ArrayList<>()).totalPrice(BigDecimal.TEN).build();
        Cart c3 = Cart.builder().userId(id2).build();
        Cart cNullId = Cart.builder().userId(null).build();
        Cart cNullId2 = Cart.builder().userId(null).build();

        // Reflexive
        assertTrue(c1.equals(c1));
        // Symmetric
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        // Different ID
        assertFalse(c1.equals(c3));
        // Null & Different class
        assertFalse(c1.equals(null));
        assertFalse(c1.equals("Some string"));
        // Null userId
        assertFalse(cNullId.equals(c1));
        assertFalse(cNullId.equals(cNullId2));

        // HashCode
        assertEquals(c1.hashCode(), c2.hashCode());
        assertEquals(c1.hashCode(), Cart.class.hashCode());

        // ToString
        assertNotNull(c1.toString());
        assertTrue(c1.toString().contains(id1.toString()));
    }

    @Test
    void testCartItemSubTotal_Branches() {
        CartItem item1 = new CartItem(UUID.randomUUID(), "Prod", "img", 3, BigDecimal.valueOf(10));
        assertEquals(BigDecimal.valueOf(30), item1.getSubTotal());

        CartItem itemNullPrice = new CartItem(UUID.randomUUID(), "Prod", "img", 3, null);
        assertEquals(BigDecimal.ZERO, itemNullPrice.getSubTotal());

        CartItem itemNullQuantity = new CartItem(UUID.randomUUID(), "Prod", "img", null, BigDecimal.TEN);
        assertEquals(BigDecimal.ZERO, itemNullQuantity.getSubTotal());

        CartItem itemBothNull = new CartItem(UUID.randomUUID(), "Prod", "img", null, null);
        assertEquals(BigDecimal.ZERO, itemBothNull.getSubTotal());
    }

    @Test
    void testCartSectionLombok() {
        UUID sellerId = UUID.randomUUID();
        CartSection s1 = new CartSection();
        s1.setSellerId(sellerId);
        s1.setSellerName("Seller A");
        s1.setSellerLogoUrl("logo.png");
        s1.setItems(new ArrayList<>());

        CartSection s2 = new CartSection(sellerId, "Seller A", "logo.png", new ArrayList<>());

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertEquals(s1.toString(), s2.toString());
        assertEquals("Seller A", s1.getSellerName());
        assertEquals("logo.png", s1.getSellerLogoUrl());
        assertEquals(sellerId, s1.getSellerId());
    }

    @Test
    void testCartItemLombok() {
        UUID prodId = UUID.randomUUID();
        CartItem item1 = new CartItem(prodId, "Name", "url", 2, BigDecimal.TEN);
        CartItem item2 = new CartItem();
        item2.setProductId(prodId);
        item2.setProductName("Name");
        item2.setProductImageUrl("url");
        item2.setQuantity(2);
        item2.setPrice(BigDecimal.TEN);

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertEquals(item1.toString(), item2.toString());
    }

    @Test
    void testDtoCoverage() {
        UUID prodId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CartItemRequestDTO reqDto = new CartItemRequestDTO(prodId, 5);
        assertEquals(prodId, reqDto.productId());
        assertEquals(5, reqDto.quantity());

        OrderPlacedEventDTO orderDto = new OrderPlacedEventDTO(orderId, userId, "user@milhub.ua", BigDecimal.valueOf(500), "Product", List.of(prodId));
        assertEquals(orderId, orderDto.orderId());
        assertEquals(userId, orderDto.userId());
        assertEquals("user@milhub.ua", orderDto.userEmail());
        assertEquals(BigDecimal.valueOf(500), orderDto.totalPrice());
        assertEquals("Product", orderDto.productName());
        assertEquals(List.of(prodId), orderDto.productIds());

        ErrorResponse err = new ErrorResponse(LocalDateTime.now(), 400, "Bad Request", "Error", "/path", Map.of("f", "e"));
        assertEquals(400, err.status());
        assertEquals("Bad Request", err.error());
        assertEquals("Error", err.message());
        assertEquals("/path", err.path());
        assertNotNull(err.timestamp());
        assertNotNull(err.validationErrors());
    }

    @Test
    void testExceptions() {
        AppException appEx = new AppException("App err", HttpStatus.BAD_REQUEST);
        assertEquals(HttpStatus.BAD_REQUEST, appEx.getStatus());
        assertEquals("App err", appEx.getMessage());

        BusinessException busEx = new BusinessException("Business err");
        assertEquals(HttpStatus.CONFLICT, busEx.getStatus());
        assertEquals("Business err", busEx.getMessage());

        ResourceNotFoundException nfEx = new ResourceNotFoundException("Not found err");
        assertEquals(HttpStatus.NOT_FOUND, nfEx.getStatus());
        assertEquals("Not found err", nfEx.getMessage());
    }
}
