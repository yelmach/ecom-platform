package ecom.gateway_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest(properties = {
        "server.port=0",
        "server.ssl.enabled=false",
        "MINIO_ENDPOINT=http://localhost:9000",
        "EUREKA_HOST=localhost",
        "EUREKA_PORT=8761",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureWebTestClient
@Import(SecurityConfigTest.TestEndpoints.class)
class SecurityConfigTest {

    private static final KeyPair SIGNING_KEY = generateKeyPair();
    private static final KeyPair OTHER_KEY = generateKeyPair();
    private static final Path PUBLIC_KEY_FILE = writePublicKey(SIGNING_KEY);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_PUBLIC_KEY_PATH", () -> PUBLIC_KEY_FILE.toAbsolutePath().toString());
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        webTestClient.get().uri("/products/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpointRejectsMalformedExpiredAndIncorrectlySignedTokens() {
        expectUnauthorized("/products/me", "not-a-jwt");
        expectUnauthorized("/products/me", token(SIGNING_KEY, "CLIENT", Instant.now().minusSeconds(60)));
        expectUnauthorized("/products/me", token(OTHER_KEY, "CLIENT", Instant.now().plusSeconds(60)));
    }

    @Test
    void clientCanUseAuthenticatedEndpointsButCannotUseSellerEndpoints() {
        String clientToken = token(SIGNING_KEY, "CLIENT", Instant.now().plusSeconds(60));

        webTestClient.get().uri("/products/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                .exchange()
                .expectStatus().isOk();

        webTestClient.patch().uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/media/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.post().uri("/media/images")
                .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void sellerCanManageProductsAndUploadProductImages() {
        String sellerToken = token(SIGNING_KEY, "SELLER", Instant.now().plusSeconds(60));

        expectAllowed(HttpMethod.POST, "/products", sellerToken);
        expectAllowed(HttpMethod.PUT, "/products/product-1", sellerToken);
        expectAllowed(HttpMethod.DELETE, "/products/product-1", sellerToken);
        expectAllowed(HttpMethod.POST, "/media/images", sellerToken);
    }

    @Test
    void invalidTokenDoesNotBlockPublicEndpoints() {
        webTestClient.get().uri("/products")
                .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt"))
                .exchange()
                .expectStatus().isOk();

        webTestClient.get().uri("/products/product-1")
                .header(HttpHeaders.AUTHORIZATION,
                        bearer(token(SIGNING_KEY, "CLIENT", Instant.now().minusSeconds(60))))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/auth/login")
                .header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void wrongMethodsAndUnknownEndpointsAreDenied() {
        webTestClient.get().uri("/auth/login").exchange().expectStatus().isUnauthorized();
        webTestClient.post().uri("/products/product-1").exchange().expectStatus().isUnauthorized();
        webTestClient.post().uri("/ecom-media/object.jpg").exchange().expectStatus().isUnauthorized();
        webTestClient.get().uri("/unknown").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void healthIsPublic() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void yamlDefinesExpectedRoutesAndRestrictsMinioMethods() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).extracting(RouteDefinition::getId)
                .contains("media-objects", "user-service", "product-service", "media-service");
        assertThat(routes).extracting(route -> route.getId() + "=" + route.getUri())
                .contains(
                        "media-objects=http://localhost:9000",
                        "user-service=lb://USER-SERVICE",
                        "product-service=lb://PRODUCT-SERVICE",
                        "media-service=lb://MEDIA-SERVICE");

        RouteDefinition mediaObjects = routes.stream()
                .filter(route -> route.getId().equals("media-objects"))
                .findFirst()
                .orElseThrow();

        assertThat(mediaObjects.getUri().toString()).isEqualTo("http://localhost:9000");
        PredicateDefinition methodPredicate = mediaObjects.getPredicates().stream()
                .filter(predicate -> predicate.getName().equals("Method"))
                .findFirst()
                .orElseThrow();
        assertThat(methodPredicate.getArgs().values()).containsExactlyInAnyOrder("GET", "HEAD");
    }

    private void expectUnauthorized(String path, String token) {
        webTestClient.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void expectAllowed(HttpMethod method, String path, String token) {
        webTestClient.method(method).uri(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus().isOk();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String token(KeyPair keyPair, String role, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("user-1")
                    .claim("email", "user@example.com")
                    .claim("role", role)
                    .issueTime(Date.from(Instant.now().minusSeconds(1)))
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
            return jwt.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create test JWT", exception);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate test RSA key", exception);
        }
    }

    private static Path writePublicKey(KeyPair keyPair) {
        try {
            Path directory = Path.of("target", "test-keys");
            Files.createDirectories(directory);
            Path publicKey = directory.resolve("public.pem").toAbsolutePath();
            String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(keyPair.getPublic().getEncoded());
            Files.writeString(publicKey,
                    "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n");
            return publicKey;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write test RSA public key", exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestEndpoints {

        @RestController
        static class TestController {

            @RequestMapping(
                    path = {
                            "/auth/login", "/auth/register", "/products", "/products/{id}",
                            "/users/me", "/media/images", "/media/images/{id}",
                            "/media/profile", "/media/profile/{id}"
                    },
                    method = {
                            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                            RequestMethod.PATCH, RequestMethod.DELETE
                    })
            void endpoint() {
            }
        }
    }
}
