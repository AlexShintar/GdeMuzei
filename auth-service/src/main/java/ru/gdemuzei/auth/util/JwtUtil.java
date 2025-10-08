package ru.gdemuzei.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private final RSAPrivateKey privateKey;

    @Getter
    private final RSAPublicKey publicKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.access-token.expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshExpiration;

    public JwtUtil(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public String generateAccessToken(String username, List<String> authorities) {
        return generateToken(username, authorities, accessExpiration, "access");
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, null, refreshExpiration, "refresh");
    }

    private String generateToken(String username, List<String> authorities, Long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .header()
                .keyId(getKeyId())
                .and()
                .subject(username)
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiryDate)
                .id(jti)
                .claim("token_type", tokenType);

        if (authorities != null && !authorities.isEmpty()) {
            builder.claim("authorities", authorities);
        }

        return builder
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .clockSkewSeconds(60)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Long getAccessExpirationTime() {
        return accessExpiration;
    }

    public String getKeyId() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray())
                .substring(0, 16);
    }
}
