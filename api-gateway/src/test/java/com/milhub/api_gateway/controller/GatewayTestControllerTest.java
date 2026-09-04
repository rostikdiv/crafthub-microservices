package com.milhub.api_gateway.controller;

import com.milhub.api_gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayTestController Tests")
class GatewayTestControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    private GatewayTestController controller;

    @BeforeEach
    void setUp() {
        controller = new GatewayTestController(jwtUtil);
    }

    @Test
    void testTestToken_NullHeader() {
        ResponseEntity<String> response = controller.testToken(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid Authorization header", response.getBody());
    }

    @Test
    void testTestToken_InvalidPrefix() {
        ResponseEntity<String> response = controller.testToken("Basic 12345");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid Authorization header", response.getBody());
    }

    @Test
    void testTestToken_ValidToken() {
        when(jwtUtil.isTokenValid("good-token")).thenReturn(true);

        ResponseEntity<String> response = controller.testToken("Bearer good-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Token is valid: true", response.getBody());
    }

    @Test
    void testTestToken_InvalidToken() {
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        ResponseEntity<String> response = controller.testToken("Bearer bad-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Token is valid: false", response.getBody());
    }

    @Test
    void testTestEndpoint() {
        ResponseEntity<String> response = controller.test();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("API Gateway service is operational.", response.getBody());
    }
}
