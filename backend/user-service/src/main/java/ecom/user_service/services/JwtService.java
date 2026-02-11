package ecom.user_service.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ecom.user_service.models.User;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyPath;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            String keyContent = Files.readString(Path.of(privateKeyPath));
            String privateKeyPem = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = keyFactory.generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load RSA private key from: " + privateKeyPath, e);
        }
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("email", user.getEmail());

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId())
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(privateKey)
                .compact();
    }
}
