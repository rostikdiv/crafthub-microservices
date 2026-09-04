package com.milhub.product_service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserContextServiceTest {

    private UserContextService userContextService;

    @BeforeEach
    void setUp() {
        userContextService = new UserContextService();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("getUserId: returns UUID when header is present")
    void getUserId_WhenHeaderPresent_ShouldReturnUUID() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", userId.toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UUID result = userContextService.getUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUserId: throws RuntimeException when header is missing")
    void getUserId_WhenHeaderMissing_ShouldThrowRuntimeException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> userContextService.getUserId())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User ID header missing");
    }

    @Test
    @DisplayName("getUserEmail: returns email header value")
    void getUserEmail_ShouldReturnHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Email", "soldier@milhub.ua");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(userContextService.getUserEmail()).isEqualTo("soldier@milhub.ua");
    }

    @Test
    @DisplayName("getUserRole: returns role header value")
    void getUserRole_ShouldReturnHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "SELLER");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(userContextService.getUserRole()).isEqualTo("SELLER");
    }

    @Test
    @DisplayName("isVerified: returns boolean based on X-User-Is-Verified header")
    void isVerified_ShouldParseBoolean() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Is-Verified", "true");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(userContextService.isVerified()).isTrue();

        request.removeHeader("X-User-Is-Verified");
        request.addHeader("X-User-Is-Verified", "false");
        assertThat(userContextService.isVerified()).isFalse();
    }

    @Test
    @DisplayName("getHeader: returns null when RequestAttributes is null")
    void getHeader_WhenNoRequestAttributes_ShouldReturnNull() {
        RequestContextHolder.resetRequestAttributes();

        assertThat(userContextService.getUserEmail()).isNull();
        assertThat(userContextService.getUserRole()).isNull();
        assertThat(userContextService.isVerified()).isFalse();
    }
}
