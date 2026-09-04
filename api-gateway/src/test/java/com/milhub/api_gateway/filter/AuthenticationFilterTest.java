package com.milhub.api_gateway.filter;

import com.milhub.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFilter Tests")
class AuthenticationFilterTest {

    @Mock
    private RouteValidator validator;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationFilter, "validator", validator);
        ReflectionTestUtils.setField(authenticationFilter, "jwtUtil", jwtUtil);
    }

    @Test
    void testApply_OpenRoute_PassesThrough() {
        validator.isSecured = request -> false;

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/authenticate").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);
        result.block();

        verify(chain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void testApply_SecuredRoute_MissingAuthorizationHeader_Throws401() {
        validator.isSecured = request -> true;

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Missing authorization header", ex.getReason());
    }

    @Test
    void testApply_SecuredRoute_InvalidAuthHeaderFormat_Throws401() {
        validator.isSecured = request -> true;

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid Authorization header format", ex.getReason());
    }

    @Test
    void testApply_SecuredRoute_NullAuthHeaderValue_Throws401() {
        validator.isSecured = request -> true;

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, (String) null)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid Authorization header format", ex.getReason());
    }

    @Test
    void testApply_SecuredRoute_TokenValidationFails_Throws401() {
        validator.isSecured = request -> true;

        String token = "invalid.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doThrow(new RuntimeException("Expired token")).when(jwtUtil).validateToken(token);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Unauthorized access", ex.getReason());
    }

    @Test
    void testApply_SecuredRoute_ValidToken_WithPermissionsAndRole() {
        validator.isSecured = request -> true;

        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("id", "user-uuid-101");
        claimsMap.put("role", "ADMIN");
        claimsMap.put("sub", "admin@milhub.ua");
        claimsMap.put("permissions", List.of("READ_PRIVILEGE", "WRITE_PRIVILEGE"));
        claimsMap.put("isVerified", true);
        Claims claims = new DefaultClaims(claimsMap);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.getAllClaimsFromToken(token)).thenReturn(claims);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            capturedExchange.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);
        result.block();

        assertNotNull(capturedExchange.get());
        HttpHeaders headers = capturedExchange.get().getRequest().getHeaders();
        assertEquals("user-uuid-101", headers.getFirst("X-User-Id"));
        assertEquals("ADMIN", headers.getFirst("X-User-Role"));
        assertEquals("admin@milhub.ua", headers.getFirst("X-User-Email"));
        assertEquals("READ_PRIVILEGE,WRITE_PRIVILEGE,ROLE_ADMIN", headers.getFirst("X-User-Permissions"));
        assertEquals("true", headers.getFirst("X-User-Is-Verified"));
    }

    @Test
    void testApply_SecuredRoute_ValidToken_NullPermissions_And_NullRole() {
        validator.isSecured = request -> true;

        String token = "valid.jwt.token2";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("id", "user-uuid-102");
        claimsMap.put("role", null);
        claimsMap.put("sub", "user@milhub.ua");
        claimsMap.put("permissions", null);
        claimsMap.put("isVerified", false);
        Claims claims = new DefaultClaims(claimsMap);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.getAllClaimsFromToken(token)).thenReturn(claims);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            capturedExchange.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);
        result.block();

        assertNotNull(capturedExchange.get());
        HttpHeaders headers = capturedExchange.get().getRequest().getHeaders();
        assertEquals("user-uuid-102", headers.getFirst("X-User-Id"));
        assertNull(headers.getFirst("X-User-Role"));
        assertEquals("user@milhub.ua", headers.getFirst("X-User-Email"));
        assertEquals("", headers.getFirst("X-User-Permissions"));
        assertEquals("false", headers.getFirst("X-User-Is-Verified"));
    }

    @Test
    void testApply_SecuredRoute_ValidToken_EmptyRole() {
        validator.isSecured = request -> true;

        String token = "valid.jwt.token3";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("id", "user-uuid-103");
        claimsMap.put("role", "");
        claimsMap.put("sub", "user@milhub.ua");
        claimsMap.put("permissions", new ArrayList<>());
        claimsMap.put("isVerified", true);
        Claims claims = new DefaultClaims(claimsMap);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.getAllClaimsFromToken(token)).thenReturn(claims);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            capturedExchange.set(invocation.getArgument(0));
            return Mono.empty();
        });

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);
        result.block();

        assertNotNull(capturedExchange.get());
        HttpHeaders headers = capturedExchange.get().getRequest().getHeaders();
        assertEquals("", headers.getFirst("X-User-Permissions"));
    }
}
