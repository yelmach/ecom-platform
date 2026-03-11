package ecom.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(jwtUtil);
        filter = authenticationFilter.apply(new AuthenticationFilter.Config());
    }

    @Test
    void shouldAllowPublicPathWithoutToken_AndStripSpoofedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/auth/login")
                .header("X-User-Id", "spoof")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = e -> {
            chainCalled.set(true);
            forwarded.set(e.getRequest());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        assertNull(forwarded.get().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void shouldAllowPublicGetPathWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products").build());

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.filter(exchange, e -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void shouldReturn401WhenProtectedPathAndMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/users/me").build());

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        filter.filter(exchange, e -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturn401WhenTokenInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        filter.filter(exchange, e -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturn403ForSellerOnlyWriteWhenRoleNotSeller() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = Jwts.claims()
                .subject("user-1")
                .add("email", "u@test.com")
                .add("role", "CLIENT")
                .build();

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractClaims("valid-token")).thenReturn(claims);

        filter.filter(exchange, e -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldForwardUserHeadersForValidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header("X-User-Id", "spoofed")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = Jwts.claims()
                .subject("seller-1")
                .add("email", "seller@test.com")
                .add("role", "SELLER")
                .build();

        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractClaims("valid-token")).thenReturn(claims);

        AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();
        filter.filter(exchange, e -> {
            forwarded.set(e.getRequest());
            return Mono.empty();
        }).block();

        assertEquals("seller-1", forwarded.get().getHeaders().getFirst("X-User-Id"));
        assertEquals("seller@test.com", forwarded.get().getHeaders().getFirst("X-User-Email"));
        assertEquals("SELLER", forwarded.get().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void shouldRequireAuthForProductsMeEvenOnGet() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products/me").build());

        filter.filter(exchange, e -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
