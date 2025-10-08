package ru.gdemuzei.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.dto.AuthRequest;
import ru.gdemuzei.auth.dto.AuthResponse;
import ru.gdemuzei.auth.dto.RefreshTokenRequest;
import ru.gdemuzei.auth.service.AuthService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request received for user: {}", request.getUsername());
        return authService.authenticate(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Authentication failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");
        return authService.refreshAccessToken(request.getRefreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Token refresh failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.logout(request.getRefreshToken())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("Auth service is running"));
    }
}
