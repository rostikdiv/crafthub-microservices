package com.milhub.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouteValidator Tests")
class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @ParameterizedTest(name = "Open endpoint {0} should not be secured")
    @ValueSource(strings = {
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/delivery/locations",
            "/api/v1/system/warmup",
            "/eureka"
    })
    void testExplicitlyOpenEndpoints_POST(String uri) {
        MockServerHttpRequest request = MockServerHttpRequest.post(uri).build();
        assertFalse(routeValidator.isSecured.test(request));
    }

    @ParameterizedTest(name = "Open endpoint {0} with GET should not be secured")
    @ValueSource(strings = {
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/delivery/locations",
            "/api/v1/system/warmup",
            "/eureka"
    })
    void testExplicitlyOpenEndpoints_GET(String uri) {
        MockServerHttpRequest request = MockServerHttpRequest.get(uri).build();
        assertFalse(routeValidator.isSecured.test(request));
    }

    @ParameterizedTest(name = "Public GET route {0} should not be secured")
    @ValueSource(strings = {
            "/api/v1/products",
            "/api/v1/products/123",
            "/api/v1/categories",
            "/api/v1/categories/sub",
            "/api/v1/sellers",
            "/api/v1/seller-reviews/seller1",
            "/seller-info/profile",
            "/api/v1/reviews/product/456"
    })
    void testPublicGetEndpoints_Allowed(String uri) {
        MockServerHttpRequest request = MockServerHttpRequest.get(uri).build();
        assertFalse(routeValidator.isSecured.test(request));
    }

    @ParameterizedTest(name = "Non-GET request to {0} should be secured")
    @ValueSource(strings = {
            "/api/v1/products",
            "/api/v1/categories",
            "/api/v1/sellers",
            "/api/v1/seller-reviews",
            "/seller-info",
            "/api/v1/reviews/product"
    })
    void testPublicEndpointsWithPOST_Secured(String uri) {
        MockServerHttpRequest request = MockServerHttpRequest.post(uri).build();
        assertTrue(routeValidator.isSecured.test(request));
    }

    @Test
    void testPublicEndpointsWithPUT_Secured() {
        MockServerHttpRequest request = MockServerHttpRequest.put("/api/v1/products/123").build();
        assertTrue(routeValidator.isSecured.test(request));
    }

    @Test
    void testPublicEndpointsWithDELETE_Secured() {
        MockServerHttpRequest request = MockServerHttpRequest.delete("/api/v1/categories/5").build();
        assertTrue(routeValidator.isSecured.test(request));
    }

    @ParameterizedTest(name = "Protected route {0} should be secured for any method")
    @ValueSource(strings = {
            "/api/v1/orders",
            "/api/v1/orders/123",
            "/api/v1/cart",
            "/api/v1/payments/checkout",
            "/api/v1/users/profile",
            "/api/v1/admin/dashboard"
    })
    void testSecuredRoutes_GET_and_POST(String uri) {
        MockServerHttpRequest getReq = MockServerHttpRequest.get(uri).build();
        assertTrue(routeValidator.isSecured.test(getReq));

        MockServerHttpRequest postReq = MockServerHttpRequest.post(uri).build();
        assertTrue(routeValidator.isSecured.test(postReq));
    }
}
