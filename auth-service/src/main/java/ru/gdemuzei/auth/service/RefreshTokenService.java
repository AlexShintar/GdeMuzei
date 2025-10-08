package ru.gdemuzei.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.exception.AuthenticationException;
import ru.gdemuzei.auth.model.RefreshToken;
import ru.gdemuzei.auth.repository.RefreshTokenRepository;
import ru.gdemuzei.auth.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtUtil jwtUtil;

    public Mono<RefreshToken> createRefreshToken(String username) {
        String tokenValue = jwtUtil.generateRefreshToken(username);
        String family = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .username(username)
                .family(family)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        return repository.save(refreshToken)
                .doOnSuccess(token -> log.debug("Created refresh token for user: {}", username));
    }

    public Mono<RefreshToken> verifyAndGetToken(String token) {
        return repository.findByToken(token)
                .switchIfEmpty(Mono.error(new AuthenticationException("Invalid refresh token")))
                .flatMap(storedToken -> {
                    if (storedToken.isRevoked()) {
                        log.warn("Attempted to use revoked token for user: {}", storedToken.getUsername());
                        // Возможная атака - отзываем всю семью
                        return repository.deleteByFamily(storedToken.getFamily())
                                .then(Mono.error(new AuthenticationException("Token has been revoked")));
                    }

                    if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                        log.warn("Expired refresh token for user: {}", storedToken.getUsername());
                        return Mono.error(new AuthenticationException("Refresh token expired"));
                    }

                    return Mono.just(storedToken);
                });
    }

    public Mono<Void> revokeToken(String token) {
        return repository.findByToken(token)
                .flatMap(refreshToken -> {
                    refreshToken.setRevoked(true);
                    return repository.save(refreshToken).then();
                });
    }

    public Mono<Void> revokeAllUserTokens(String username) {
        return repository.findAll()
                .filter(token -> token.getUsername().equals(username))
                .flatMap(token -> {
                    token.setRevoked(true);
                    return repository.save(token);
                })
                .then()
                .doOnSuccess(v -> log.info("Revoked all tokens for user: {}", username));
    }
}
