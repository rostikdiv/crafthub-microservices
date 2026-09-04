package com.milhub.cart_service;

import com.milhub.cart_service.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceApplicationTests {

    @MockBean
    private CartRepository cartRepository;

    @Test
    void contextLoads() {
    }
}
