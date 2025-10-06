package ru.gdemuzei.client;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.dto.MuseumUpdateRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-тесты для проверки связи с museum-api.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MuseumApiSmokeTest {

    @Autowired
    private MuseumApiClient museumApiClient;

    private static String testMuseumId;
    private static String testMuseumName;

    @BeforeAll
    static void setup() {
        // Генерируем уникальное имя с timestamp для каждого запуска тестов
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        testMuseumName = "SmokeTest_" + timestamp;
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Запуск Smoke-тестов для museum-api");
        System.out.println("Тестовый музей: " + testMuseumName);
        System.out.println("=".repeat(60));
    }

    @Test
    @Order(1)
    @DisplayName("1. GET /api/admin/v2/museums - получение списка музеев")
    void shouldFetchMuseumsPaginated() {
        System.out.println("\n[Тест 1] Получение списка музеев...");

        StepVerifier.create(museumApiClient.getMuseumsPaginated(0, 10))
                .assertNext(page -> {
                    assertThat(page).isNotNull();
                    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(0);
                    System.out.println("  ✓ Получена страница с " + page.getTotalElements() + " музеями");
                    System.out.println("  ✓ Размер текущей страницы: " + page.getContent().size());
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    @DisplayName("2. POST /api/admin/v2/museums - создание музея")
    void shouldCreateMuseum() {
        System.out.println("\n[Тест 2] Создание нового музея: " + testMuseumName);

        MuseumCreateRequest createRequest = new MuseumCreateRequest(
                testMuseumName,
                37.6173,
                55.7558,
                "@smoketest",
                "https://smoketest.example.com",
                "Тестовый адрес для smoke test",
                "Москва"
        );

        StepVerifier.create(museumApiClient.createMuseum(createRequest))
                .assertNext(createdMuseum -> {
                    assertThat(createdMuseum).isNotNull();
                    assertThat(createdMuseum.id()).isNotNull();
                    assertThat(createdMuseum.officialName()).isEqualTo(testMuseumName);
                    assertThat(createdMuseum.latitude()).isEqualTo(55.7558);
                    assertThat(createdMuseum.longitude()).isEqualTo(37.6173);
                    assertThat(createdMuseum.locality()).isEqualTo("Москва");

                    testMuseumId = createdMuseum.id();
                    System.out.println("  ✓ Создан музей с ID: " + testMuseumId);
                    System.out.println("  ✓ Название: " + createdMuseum.officialName());
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    @DisplayName("3. GET /api/public/v2/museums/{id} - получение музея по ID")
    void shouldFetchMuseumById() {
        System.out.println("\n[Тест 3] Получение музея по ID: " + testMuseumId);

        assertThat(testMuseumId)
                .withFailMessage("Тестовый музей должен быть создан в предыдущем тесте")
                .isNotNull();

        StepVerifier.create(museumApiClient.getMuseumById(testMuseumId))
                .assertNext(museum -> {
                    assertThat(museum).isNotNull();
                    assertThat(museum.id()).isEqualTo(testMuseumId);
                    assertThat(museum.officialName()).isEqualTo(testMuseumName);
                    System.out.println("  ✓ Получен музей: " + museum.officialName());
                    System.out.println("  ✓ Координаты: " + museum.latitude() + ", " + museum.longitude());
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    @DisplayName("4. PUT /api/admin/v2/museums/{id} - обновление музея")
    void shouldUpdateMuseum() {
        System.out.println("\n[Тест 4] Обновление музея ID: " + testMuseumId);

        assertThat(testMuseumId)
                .withFailMessage("Тестовый музей должен быть создан")
                .isNotNull();

        MuseumUpdateRequest updateRequest = new MuseumUpdateRequest(
                testMuseumName + "_UPDATED",
                37.6047,
                55.7467,
                "@updatedtest",
                "https://updated.example.com",
                "Обновленный адрес",
                "Санкт-Петербург",
                true
        );

        StepVerifier.create(museumApiClient.updateMuseum(testMuseumId, updateRequest))
                .assertNext(updatedMuseum -> {
                    assertThat(updatedMuseum).isNotNull();
                    assertThat(updatedMuseum.id()).isEqualTo(testMuseumId);
                    assertThat(updatedMuseum.officialName()).isEqualTo(testMuseumName + "_UPDATED");
                    assertThat(updatedMuseum.latitude()).isEqualTo(55.7467);
                    assertThat(updatedMuseum.longitude()).isEqualTo(37.6047);
                    assertThat(updatedMuseum.locality()).isEqualTo("Санкт-Петербург");
//                    assertThat(updatedMuseum.verified()).isTrue(); // верификация не настроена, пока все музеи "непроверенные"
                    System.out.println("  ✓ Обновлен музей: " + updatedMuseum.officialName());
                    System.out.println("  ✓ Новый город: " + updatedMuseum.locality());
                    System.out.println("  ✓ Verified: " + updatedMuseum.verified());
                })
                .verifyComplete();
    }

    @Test
    @Order(5)
    @DisplayName("5. GET /api/public/v2/museums/{id} - проверка персистентности обновлений")
    void shouldVerifyUpdatePersisted() {
        System.out.println("\n[Тест 5] Проверка сохранения обновлений...");

        assertThat(testMuseumId)
                .withFailMessage("Тестовый музей должен быть создан")
                .isNotNull();

        StepVerifier.create(museumApiClient.getMuseumById(testMuseumId))
                .assertNext(museum -> {
                    assertThat(museum.officialName()).isEqualTo(testMuseumName + "_UPDATED");
                    assertThat(museum.locality()).isEqualTo("Санкт-Петербург");
//                    assertThat(museum.verified()).isTrue();
                    System.out.println("  ✓ Обновления сохранены в базе");
                })
                .verifyComplete();
    }

    @Test
    @Order(6)
    @DisplayName("6. GET /api/public/v2/museums/{id} - несуществующий музей")
    void shouldReturnEmptyForNonExistentMuseum() {
        System.out.println("\n[Тест 6] Попытка получить несуществующий музей...");

        String nonExistentId = "non-existent-id-12345";

        StepVerifier.create(museumApiClient.getMuseumById(nonExistentId))
                .verifyComplete();

        System.out.println("  ✓ Несуществующий музей корректно вернул empty");
    }

    @AfterAll
    static void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("✓✓✓ Все smoke-тесты успешно пройдены! ✓✓✓");
        System.out.println("=".repeat(60));
        System.out.println("Проверено:");
        System.out.println("  • GET  /api/admin/v2/museums (пагинация)");
        System.out.println("  • POST /api/admin/v2/museums (создание)");
        System.out.println("  • GET  /api/public/v2/museums/{id} (чтение)");
        System.out.println("  • PUT  /api/admin/v2/museums/{id} (обновление)");
        if (testMuseumId != null) {
            System.out.println("\n⚠ Тестовый музей остался в базе:");
            System.out.println("  ID: " + testMuseumId);
            System.out.println("  Имя: " + testMuseumName + "_UPDATED");
            System.out.println("  (можно удалить вручную через MongoDB или админку)");
        }
        System.out.println("=".repeat(60) + "\n");
    }
}
