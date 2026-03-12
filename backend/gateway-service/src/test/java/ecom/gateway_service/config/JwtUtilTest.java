package ecom.gateway_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

class JwtUtilTest {

    @Test
    void init_ShouldThrowWhenPublicKeyPathInvalid() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKeyPath", "/does/not/exist/public.pem");

        assertThrows(IllegalStateException.class, jwtUtil::init);
    }

    @Test
    void extractClaims_AndIsTokenValid_ShouldWorkForValidToken() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Path publicKeyFile = Files.createTempFile("gateway-public", ".pem");
        Files.writeString(publicKeyFile, publicPem);

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKeyPath", publicKeyFile.toString());
        jwtUtil.init();

        String token = Jwts.builder()
                .subject("user-1")
                .claims(Map.of("email", "u@test.com", "role", "SELLER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate())
                .compact();

        Claims claims = jwtUtil.extractClaims(token);

        assertEquals("user-1", claims.getSubject());
        assertEquals("u@test.com", claims.get("email", String.class));
        assertEquals("SELLER", claims.get("role", String.class));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalseForInvalidToken() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        Path publicKeyFile = Files.createTempFile("gateway-public", ".pem");
        Files.writeString(publicKeyFile, publicPem);

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "publicKeyPath", publicKeyFile.toString());
        jwtUtil.init();

        assertFalse(jwtUtil.isTokenValid("not-a-jwt"));
    }
}
