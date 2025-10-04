package ru.gdemuzei.auth.config;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Configuration
public class KeyPairConfig {

    @Bean
    public KeyPair keyPair() {
        // Генерация RSA ключевой пары для RS256
        KeyPair keyPair = Jwts.SIG.RS256.keyPair().build();
        log.info("Generated RSA key pair for JWT signing");
        return keyPair;
    }

    @Bean
    public RSAPublicKey publicKey(KeyPair keyPair) {
        return (RSAPublicKey) keyPair.getPublic();
    }

    @Bean
    public RSAPrivateKey privateKey(KeyPair keyPair) {
        return (RSAPrivateKey) keyPair.getPrivate();
    }
}
