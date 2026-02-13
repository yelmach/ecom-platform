package ecom.gateway_service.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final String internalGatewayKey;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register"
    );

    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/products",
            "/media/images"
    );

    private static final List<String> SELLER_PATHS = List.of(
            "/products",
            "/media/images"
    );

    public AuthenticationFilter(JwtUtil jwtUtil, @Value("${internal.gateway-key}") String internalGatewayKey) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.internalGatewayKey = internalGatewayKey;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            // Strip spoofed headers from all requests
            ServerHttpRequest baseRequest = request.mutate().headers(headers -> {
                headers.remove("X-User-Id");
                headers.remove("X-User-Email");
                headers.remove("X-User-Role");
                headers.remove("X-Internal-Gateway-Key");
                headers.set("X-Internal-Gateway-Key", internalGatewayKey);
            }).build();

            // Skip authentication for public endpoints
            if (isPublicPath(path)) {
                return chain.filter(exchange.mutate().request(baseRequest).build());
            }

            // Allow unauthenticated GET on public read endpoints
            if (method == HttpMethod.GET && isPublicGetPath(path)) {
                return chain.filter(exchange.mutate().request(baseRequest).build());
            }

            // All other requests require a valid token
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange);
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                return unauthorized(exchange);
            }

            Claims claims = jwtUtil.extractClaims(token);
            String role = claims.get("role", String.class);

            // Seller-only write endpoints
            if (isSellerOnly(path, method) && !"SELLER".equals(role)) {
                return forbidden(exchange);
            }

            // Forward user claims as headers to downstream services
            ServerHttpRequest authenticatedRequest = baseRequest.mutate().headers(headers -> {
                headers.set("X-User-Id", claims.getSubject());
                headers.set("X-User-Email", claims.get("email", String.class));
                headers.set("X-User-Role", role);
            }).build();

            return chain.filter(exchange.mutate().request(authenticatedRequest).build());
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isPublicGetPath(String path) {
        if (path.equals("/products/me")) {
            return false;
        }
        return PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isSellerOnly(String path, HttpMethod method) {
        if (method == HttpMethod.GET) {
            return false;
        }
        return SELLER_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
