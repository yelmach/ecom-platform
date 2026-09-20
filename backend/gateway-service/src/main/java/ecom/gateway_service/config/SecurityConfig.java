package ecom.gateway_service.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    private static final String SELLER = "SELLER";

    @Bean
    @Order(1)
    SecurityWebFilterChain protectedEndpointsSecurity(
            ServerHttpSecurity http,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter) {
        return statelessApi(http)
                .securityMatcher(protectedEndpoints())
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.POST, "/products", "/media/images").hasRole(SELLER)
                        .pathMatchers(HttpMethod.PUT, "/products/*").hasRole(SELLER)
                        .pathMatchers(HttpMethod.DELETE, "/products/*").hasRole(SELLER)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    @Order(2)
    SecurityWebFilterChain publicEndpointsSecurity(ServerHttpSecurity http) {
        return statelessApi(http)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/products", "/products/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/media/images/*", "/media/profile/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/ecom-media/**", "/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.HEAD, "/ecom-media/**").permitAll()
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(authenticationConverter);
    }

    private ServerWebExchangeMatcher protectedEndpoints() {
        return new OrServerWebExchangeMatcher(
                pathMatchers(HttpMethod.GET, "/products/me", "/users/me"),
                pathMatchers(HttpMethod.PATCH, "/users/me"),
                pathMatchers(HttpMethod.POST, "/products", "/media/images", "/media/profile"),
                pathMatchers(HttpMethod.PUT, "/products/*"),
                pathMatchers(HttpMethod.DELETE, "/products/*"));
    }

    private ServerHttpSecurity statelessApi(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .requestCache(requestCache -> requestCache.disable())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance());
    }
}
