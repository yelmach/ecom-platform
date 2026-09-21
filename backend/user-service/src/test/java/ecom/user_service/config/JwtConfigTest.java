package ecom.user_service.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtConfigTest {

    @Test
    void jwtEncoder_LoadsPemKeyPair() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Path keyDirectory = Files.createTempDirectory("jwt-key-pair");
        Path publicKey = writePem(keyDirectory.resolve("public.pem"), "PUBLIC KEY", keyPair.getPublic().getEncoded());
        Path privateKey = writePem(keyDirectory.resolve("private.pem"), "PRIVATE KEY",
                keyPair.getPrivate().getEncoded());

        JwtEncoder encoder = new JwtConfig().jwtEncoder(
                new FileSystemResource(publicKey),
                new FileSystemResource(privateKey));

        assertNotNull(encoder);
    }

    @Test
    void jwtEncoder_FailsClearlyWhenKeyCannotBeRead() {
        FileSystemResource missingKey = new FileSystemResource("target/missing-key.pem");

        assertThrows(
                IllegalStateException.class,
                () -> new JwtConfig().jwtEncoder(missingKey, missingKey));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static Path writePem(Path path, String type, byte[] key) throws Exception {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key);
        return Files.writeString(path, "-----BEGIN " + type + "-----\n"
                + encoded
                + "\n-----END " + type + "-----\n");
    }
}
