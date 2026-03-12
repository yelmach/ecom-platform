package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ecom.user_service.models.Role;
import ecom.user_service.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtServiceTest {

    @Test
    void init_ShouldThrowWhenKeyPathIsInvalid() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "privateKeyPath", "/path/that/does/not/exist.pem");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60000L);

        assertThrows(IllegalStateException.class, jwtService::init);
    }

    @Test
    void generateToken_ShouldContainExpectedClaimsAndSubject() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        Path privateKeyFile = Files.createTempFile("jwt-private-key", ".pem");
        Files.writeString(privateKeyFile, privatePem);

        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "privateKeyPath", privateKeyFile.toString());
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        jwtService.init();

        User user = new User();
        user.setId("user-123");
        user.setEmail("user@test.com");
        user.setRole(Role.CLIENT);

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        Claims claims = Jwts.parser()
                .verifyWith((RSAPublicKey) keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("user-123", claims.getSubject());
        assertEquals("user@test.com", claims.get("email"));
        assertEquals("CLIENT", claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}
