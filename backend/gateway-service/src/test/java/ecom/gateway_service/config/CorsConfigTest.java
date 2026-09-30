package ecom.gateway_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;

import reactor.core.publisher.Mono;

class CorsConfigTest {

    @Test
    void allowsConfiguredPreflightRequest() {
        assertPreflightAllowed("GET");
    }

    @Test
    void allowsHeadRequestsUsedByTheMediaRoute() {
        assertPreflightAllowed("HEAD");
    }

    private void assertPreflightAllowed(String requestedMethod) {
        CorsConfig corsConfig = new CorsConfig("https://localhost:4200, http://localhost:4200");
        CorsWebFilter corsWebFilter = new CorsWebFilter(corsConfig.corsConfigurationSource());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, requestedMethod)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        corsWebFilter.filter(exchange, ignored -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:4200");
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .contains(requestedMethod);
    }
}
