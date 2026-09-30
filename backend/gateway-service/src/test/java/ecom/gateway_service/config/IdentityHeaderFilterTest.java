package ecom.gateway_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class IdentityHeaderFilterTest {

    private final IdentityHeaderFilter filter = new IdentityHeaderFilter();

    @Test
    void stripsSpoofedIdentityHeadersFromAnonymousRequests() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products")
                .header(IdentityHeaderFilter.USER_ID_HEADER, "spoofed-id")
                .header(IdentityHeaderFilter.USER_EMAIL_HEADER, "spoofed@example.com")
                .header(IdentityHeaderFilter.USER_ROLE_HEADER, "SELLER")
                .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        filter.filter(exchange, capture(forwardedRequest)).block();

        assertThat(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.USER_ID_HEADER)).isNull();
        assertThat(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.USER_EMAIL_HEADER)).isNull();
        assertThat(forwardedRequest.get().getHeaders().getFirst(IdentityHeaderFilter.USER_ROLE_HEADER)).isNull();
    }

    @Test
    void replacesSpoofedHeadersWithJwtClaimsAndPreservesAuthorization() {
        String authorization = "Bearer original-token";
        Jwt jwt = new Jwt(
                "original-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"),
                Map.of("sub", "user-1", "email", "user@example.com", "role", "SELLER"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/products/me")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header(IdentityHeaderFilter.USER_ID_HEADER, "spoofed-id")
                        .build())
                .mutate()
                .principal(Mono.just(authentication))
                .build();
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        filter.filter(exchange, capture(forwardedRequest)).block();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        assertThat(headers.getFirst(IdentityHeaderFilter.USER_ID_HEADER)).isEqualTo("user-1");
        assertThat(headers.getFirst(IdentityHeaderFilter.USER_EMAIL_HEADER)).isEqualTo("user@example.com");
        assertThat(headers.getFirst(IdentityHeaderFilter.USER_ROLE_HEADER)).isEqualTo("SELLER");
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(authorization);
    }

    private GatewayFilterChain capture(AtomicReference<ServerHttpRequest> request) {
        return exchange -> {
            request.set(exchange.getRequest());
            return Mono.empty();
        };
    }
}
