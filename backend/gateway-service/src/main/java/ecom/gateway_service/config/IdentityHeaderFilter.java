package ecom.gateway_service.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_EMAIL_HEADER = "X-User-Email";
    static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitize(exchange.getRequest()))
                .build();

        return sanitizedExchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> withIdentityHeaders(sanitizedExchange, authentication))
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    private ServerHttpRequest sanitize(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            headers.remove(USER_ID_HEADER);
            headers.remove(USER_EMAIL_HEADER);
            headers.remove(USER_ROLE_HEADER);
        }).build();
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, JwtAuthenticationToken authentication) {
        return exchange.mutate().request(exchange.getRequest().mutate().headers(headers -> {
            setIfPresent(headers, USER_ID_HEADER, authentication.getToken().getSubject());
            setIfPresent(headers, USER_EMAIL_HEADER, authentication.getToken().getClaimAsString("email"));
            setIfPresent(headers, USER_ROLE_HEADER, authentication.getToken().getClaimAsString("role"));
        }).build()).build();
    }

    private void setIfPresent(HttpHeaders headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
