package ru.gdemuzei;

import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayAuthSmokeTest {

    private static final String GATEWAY_URL = "http://localhost:8080";
    private static final String AUTH_SERVICE_URL = "http://localhost:8083";

    @Test
    @Order(1)
    @DisplayName("1. Auth-service напрямую доступен")
    void authServiceIsAvailable() {
        System.out.println("\n[Тест 1] Проверка auth-service напрямую (порт 8083)...");

        WebClient healthClient = WebClient.create(AUTH_SERVICE_URL);
        String health = healthClient.get()
                .uri("/auth/health")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(health)
                .withFailMessage("Auth-service не отвечает на /auth/health")
                .isNotNull();

        String token = getTokenDirect(AUTH_SERVICE_URL);
        assertThat(token)
                .withFailMessage("Auth-service не выдал токен на /auth/login")
                .isNotNull();

        System.out.println("  ✓ Auth-service работает и выдаёт токены");
    }

    @Test
    @Order(2)
    @DisplayName("2. Gateway доступен")
    void gatewayIsAvailable() {
        System.out.println("\n[Тест 2] Проверка Gateway (порт 8080)...");

        WebClient client = WebClient.create(GATEWAY_URL);
        String response = client.get()
                .uri("/.well-known/jwks.json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(response)
                .withFailMessage("Gateway не отдаёт /.well-known/jwks.json")
                .isNotNull()
                .contains("keys");

        System.out.println("  ✓ Gateway отвечает на /.well-known/jwks.json");
    }

    @Test
    @Order(3)
    @DisplayName("3. Gateway разрешает /auth/login без токена")
    void gatewayAllowsAuthLogin() {
        System.out.println("\n[Тест 3] Проверка /auth/login через Gateway...");

        String token = getTokenDirect(GATEWAY_URL);
        assertThat(token)
                .withFailMessage("Gateway не пропустил запрос к /auth/login")
                .isNotNull();

        System.out.println("  ✓ Gateway пропускает /auth/login");
    }

    @Test
    @Order(4)
    @DisplayName("4. Gateway блокирует /api/admin без токена")
    void gatewayBlocksAdminWithoutToken() {
        System.out.println("\n[Тест 4] Проверка /api/admin без токена...");

        WebClient client = WebClient.create(GATEWAY_URL);
        try {
            client.get()
                    .uri("/api/admin/v2/museums?page=0&size=10")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Assertions.fail("Gateway пустил запрос в /api/admin без токена");
        } catch (WebClientResponseException e) {
            assertThat(e.getStatusCode().value())
                    .withFailMessage("Gateway вернул неожиданный код при доступе без токена")
                    .isIn(401, 403);
            System.out.println("  ✓ Gateway блокирует /api/admin без токена");
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Gateway пропускает /api/admin с токеном")
    void gatewayAllowsAdminWithToken() {
        System.out.println("\n[Тест 5] Проверка /api/admin с токеном...");

        String token = getTokenDirect(GATEWAY_URL);
        assertThat(token)
                .withFailMessage("Не удалось получить токен через Gateway")
                .isNotNull();

        WebClient client = WebClient.create(GATEWAY_URL);
        String response = client.get()
                .uri("/api/admin/v2/museums?page=0&size=10")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertThat(response)
                .withFailMessage("Gateway не пропустил запрос с валидным токеном")
                .isNotNull();

        System.out.println("  ✓ Gateway пропустил /api/admin с токеном");
    }

    private String getTokenDirect(String baseUrl) {
        WebClient client = WebClient.create(baseUrl);

        LoginResponse response = client.post()
                .uri("/auth/login")
                .bodyValue(new LoginRequest("admin1", "admin123"))
                .retrieve()
                .bodyToMono(LoginResponse.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    System.err.println("Ошибка при получении токена: HTTP " + e.getStatusCode());
                    return Mono.empty();
                })
                .block();

        return response != null ? response.accessToken() : null;
    }

    record LoginRequest(String username, String password) {}
    record LoginResponse(String accessToken, String refreshToken,
                         String username, long expiresIn) {}

    @AfterAll
    static void summary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Smoke-тест Gateway/Auth завершён");
        System.out.println("=".repeat(60) + "\n");
    }
}
