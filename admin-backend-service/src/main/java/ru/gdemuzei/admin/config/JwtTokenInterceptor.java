package ru.gdemuzei.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtTokenInterceptor implements ExchangeFilterFunction {

    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0;

    @Value("${auth.username:admin1}")
    private String username;

    @Value("${auth.password:admin123}")
    private String password;

    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Пропускаем публичные эндпоинты
        if (request.url().getPath().contains("/api/public/")) {
            return next.exchange(request);
        }

        return getValidToken()
                .flatMap(token -> {
                    ClientRequest authorizedRequest = ClientRequest.from(request)
                            .header("Authorization", "Bearer " + token)
                            .build();
                    return next.exchange(authorizedRequest);
                });
    }

    private Mono<String> getValidToken() {
        // Если токен еще действителен (с запасом 1 минута)
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt - 60000) {
            return Mono.just(cachedToken);
        }

        // Получаем новый токен
        return authenticateAndCache();
    }

    private Mono<String> authenticateAndCache() {
        log.info("Получение JWT токена для admin-backend");

        return org.springframework.web.reactive.function.client.WebClient.create(authServiceUrl)
                .post()
                .uri("/auth/login")
                .bodyValue(new LoginRequest(username, password))
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .doOnNext(response -> {
                    this.cachedToken = response.accessToken();
                    // expiresIn в миллисекундах
                    this.tokenExpiresAt = System.currentTimeMillis() + response.expiresIn();
                    log.info("JWT токен получен и закеширован на {} мс", response.expiresIn());
                })
                .map(LoginResponse::accessToken);
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String accessToken, String refreshToken,
                                String username, long expiresIn) {}
}
