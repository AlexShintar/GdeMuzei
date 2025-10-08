package ru.gdemuzei.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.dto.AuthRequest;
import ru.gdemuzei.auth.dto.AuthResponse;
import ru.gdemuzei.auth.exception.AuthenticationException;
import ru.gdemuzei.auth.repository.UserRepository;
import ru.gdemuzei.auth.util.JwtUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public Mono<AuthResponse> authenticate(AuthRequest request) {
        log.info("Attempting authentication for user: {}", request.getUsername());

        return userRepository.findByUsername(request.getUsername())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", request.getUsername());
                    return Mono.error(new AuthenticationException("Invalid username or password"));
                }))
                .flatMap(user -> {
                    if (!user.isEnabled()) {
                        log.warn("User account disabled: {}", request.getUsername());
                        return Mono.error(new AuthenticationException("Account is disabled"));
                    }

                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        log.warn("Invalid password for user: {}", request.getUsername());
                        return Mono.error(new AuthenticationException("Invalid username or password"));
                    }

                    user.setLastLogin(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .flatMap(user -> {
                    String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getAuthorities());

                    return refreshTokenService.createRefreshToken(user.getUsername())
                            .map(refreshToken -> AuthResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(refreshToken.getToken())
                                    .username(user.getUsername())
                                    .authorities(user.getAuthorities())
                                    .expiresIn(jwtUtil.getAccessExpirationTime())
                                    .build());
                })
                .doOnSuccess(response ->
                        log.info("Successfully authenticated user: {}", response.getUsername())
                );
    }

    public Mono<AuthResponse> refreshAccessToken(String refreshToken) {
        return refreshTokenService.verifyAndGetToken(refreshToken)
                .flatMap(storedToken -> {
                    // Отзываем старый токен (ротация)
                    storedToken.setRevoked(true);
                    return refreshTokenService.revokeToken(storedToken.getToken())
                            .then(userRepository.findByUsername(storedToken.getUsername()));
                })
                .flatMap(user -> {
                    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getAuthorities());

                    return refreshTokenService.createRefreshToken(user.getUsername())
                            .map(newRefreshToken -> AuthResponse.builder()
                                    .accessToken(newAccessToken)
                                    .refreshToken(newRefreshToken.getToken())
                                    .username(user.getUsername())
                                    .authorities(user.getAuthorities())
                                    .expiresIn(jwtUtil.getAccessExpirationTime())
                                    .build());
                })
                .doOnSuccess(response ->
                        log.info("Refreshed access token for user: {}", response.getUsername())
                );
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenService.revokeToken(refreshToken)
                .doOnSuccess(v -> log.info("User logged out"));
    }
}
