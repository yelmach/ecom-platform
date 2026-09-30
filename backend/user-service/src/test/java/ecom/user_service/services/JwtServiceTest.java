package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import ecom.user_service.models.Role;
import ecom.user_service.models.User;

class JwtServiceTest {

    @Test
    void generateToken_ShouldCreateGatewayCompatibleRs256Token() throws Exception {
        KeyPair keyPair = generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        Duration expiration = Duration.ofHours(1);
        JwtService jwtService = new JwtService(
                NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build(),
                expiration);

        User user = new User();
        user.setId("user-123");
        user.setEmail("user@test.com");
        user.setRole(Role.CLIENT);

        Instant beforeIssuing = Instant.now();
        String token = jwtService.generateToken(user);
        Jwt decoded = NimbusJwtDecoder.withPublicKey(publicKey).build().decode(token);

        assertNotNull(token);
        assertEquals("RS256", decoded.getHeaders().get("alg"));
        assertEquals("user-123", decoded.getSubject());
        assertEquals("user@test.com", decoded.getClaimAsString("email"));
        assertEquals("CLIENT", decoded.getClaimAsString("role"));
        assertNotNull(decoded.getIssuedAt());
        assertNotNull(decoded.getExpiresAt());
        assertTrue(!decoded.getIssuedAt().isBefore(beforeIssuing.minusSeconds(1)));
        assertEquals(expiration, Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt()));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
