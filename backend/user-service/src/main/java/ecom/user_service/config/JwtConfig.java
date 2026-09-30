package ecom.user_service.config;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    @Bean
    JwtEncoder jwtEncoder(@Value("${jwt.public-key}") Resource publicKeyResource,
            @Value("${jwt.private-key}") Resource privateKeyResource) {

        try {
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privateKeyResource.getInputStream());
            return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load JWT RSA key pair", exception);
        }
    }
}
