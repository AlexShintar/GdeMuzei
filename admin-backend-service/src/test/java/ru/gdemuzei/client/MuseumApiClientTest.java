package ru.gdemuzei.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.dto.MuseumSummaryDto;
import ru.gdemuzei.dto.MuseumUpdateRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MuseumApiClientTest {

    private MockWebServer mockWebServer;
    private MuseumApiClient museumApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        museumApiClient = new MuseumApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getMuseumsPaginated_shouldReturnPageOfMuseums() throws InterruptedException {
        // Arrange
        String responseBody = """
            {
              "content": [
                {
                  "id": "1",
                  "name": "Эрмитаж",
                  "locality": "Санкт-Петербург",
                  "address": "Дворцовая площадь, 2",
                  "verified": true,
                  "website": "https://www.hermitagemuseum.org"
                }
              ],
              "pageable": {
                "pageNumber": 0,
                "pageSize": 20
              },
              "totalElements": 1,
              "totalPages": 1,
              "last": true,
              "first": true,
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "sort": {}
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        // Act
        StepVerifier.create(museumApiClient.getMuseumsPaginated(0, 20))
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(1);
                    assertThat(page.getContent()).hasSize(1);

                    MuseumSummaryDto museum = page.getContent().get(0);
                    assertThat(museum.id()).isEqualTo("1");
                    assertThat(museum.name()).isEqualTo("Эрмитаж");
                    assertThat(museum.locality()).isEqualTo("Санкт-Петербург");
                    assertThat(museum.verified()).isTrue();
                })
                .verifyComplete();

        // Assert
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/admin/v2/museums?page=0&size=20");
    }

    @Test
    void getMuseumById_whenMuseumExists_shouldReturnMuseum() throws InterruptedException {
        // Arrange
        String museumId = "museum-123";
        String responseBody = """
            {
              "id": "museum-123",
              "officialName": "Третьяковская галерея",
              "locality": "Москва",
              "addressOverride": "Лаврушинский переулок, 10",
              "website": "https://www.tretyakovgallery.ru",
              "telegram": "@tretyakov",
              "adminTags": ["art", "painting"],
              "verified": true,
              "osmData": null,
              "latitude": 55.7414,
              "longitude": 37.6207
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        // Act
        StepVerifier.create(museumApiClient.getMuseumById(museumId))
                .assertNext(museum -> {
                    assertThat(museum.id()).isEqualTo("museum-123");
                    assertThat(museum.officialName()).isEqualTo("Третьяковская галерея");
                    assertThat(museum.locality()).isEqualTo("Москва");
                    assertThat(museum.verified()).isTrue();
                    assertThat(museum.latitude()).isEqualTo(55.7414);
                    assertThat(museum.longitude()).isEqualTo(37.6207);
                })
                .verifyComplete();

        // Assert
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/public/v2/museums/" + museumId);
    }

    @Test
    void getMuseumById_whenMuseumNotFound_shouldReturnEmpty() throws InterruptedException {
        // Arrange
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Museum not found\"}"));

        // Act
        StepVerifier.create(museumApiClient.getMuseumById("nonexistent-id"))
                .verifyComplete();

        // Assert
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/public/v2/museums/nonexistent-id");
    }

    @Test
    void createMuseum_withValidRequest_shouldReturnCreatedMuseum() throws Exception {
        // Arrange
        MuseumCreateRequest createRequest = new MuseumCreateRequest(
                "Пушкинский музей",
                37.6047,
                55.7467,
                "@pushkinmuseum",
                "https://www.pushkinmuseum.art",
                "ул. Волхонка, 12",
                "Москва"
        );

        String responseBody = """
            {
              "id": "new-museum-456",
              "officialName": "Пушкинский музей",
              "locality": "Москва",
              "addressOverride": "ул. Волхонка, 12",
              "website": "https://www.pushkinmuseum.art",
              "telegram": "@pushkinmuseum",
              "adminTags": [],
              "verified": false,
              "osmData": null,
              "latitude": 55.7467,
              "longitude": 37.6047
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        // Act
        StepVerifier.create(museumApiClient.createMuseum(createRequest))
                .assertNext(museum -> {
                    assertThat(museum.id()).isEqualTo("new-museum-456");
                    assertThat(museum.officialName()).isEqualTo("Пушкинский музей");
                    assertThat(museum.locality()).isEqualTo("Москва");
                    assertThat(museum.verified()).isFalse();
                })
                .verifyComplete();

        // Assert
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/admin/v2/museums");
        assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).contains(MediaType.APPLICATION_JSON_VALUE);

        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("Пушкинский музей");
        assertThat(requestBody).contains("Москва");
    }

    @Test
    void createMuseum_whenServerReturnsError_shouldThrowException() throws InterruptedException {
        // Arrange
        MuseumCreateRequest createRequest = new MuseumCreateRequest(
                "Invalid Museum",
                200.0,
                100.0,
                null,
                null,
                null,
                null
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Invalid coordinates\"}"));

        // Act & Assert
        StepVerifier.create(museumApiClient.createMuseum(createRequest))
                .expectErrorMatches(throwable ->
                        throwable instanceof MuseumApiClient.ApiClientException &&
                                ((MuseumApiClient.ApiClientException) throwable).getStatus().equals(HttpStatus.BAD_REQUEST) &&
                                throwable.getMessage().contains("Invalid coordinates")
                )
                .verify();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    void updateMuseum_withValidRequest_shouldReturnUpdatedMuseum() throws Exception {
        // Arrange
        String museumId = "museum-789";
        MuseumUpdateRequest updateRequest = new MuseumUpdateRequest(
                "Обновленное название",
                37.6047,
                55.7467,
                "@newhandle",
                "https://new-website.com",
                "Новый адрес",
                "Москва",
                true
        );

        String responseBody = """
            {
              "id": "museum-789",
              "officialName": "Обновленное название",
              "locality": "Москва",
              "addressOverride": "Новый адрес",
              "website": "https://new-website.com",
              "telegram": "@newhandle",
              "adminTags": [],
              "verified": true,
              "osmData": null,
              "latitude": 55.7467,
              "longitude": 37.6047
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        // Act
        StepVerifier.create(museumApiClient.updateMuseum(museumId, updateRequest))
                .assertNext(museum -> {
                    assertThat(museum.id()).isEqualTo("museum-789");
                    assertThat(museum.officialName()).isEqualTo("Обновленное название");
                    assertThat(museum.verified()).isTrue();
                    assertThat(museum.website()).isEqualTo("https://new-website.com");
                })
                .verifyComplete();

        // Assert
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/api/admin/v2/museums/" + museumId);
        assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void updateMuseum_whenMuseumNotFound_shouldThrowException() throws InterruptedException {
        // Arrange
        MuseumUpdateRequest updateRequest = new MuseumUpdateRequest(
                "Test",
                37.6047,
                55.7467,
                null,
                null,
                null,
                null,
                false
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Museum not found\"}"));

        // Act & Assert
        StepVerifier.create(museumApiClient.updateMuseum("nonexistent", updateRequest))
                .expectErrorMatches(throwable ->
                        throwable instanceof MuseumApiClient.ApiClientException &&
                                ((MuseumApiClient.ApiClientException) throwable).getStatus().equals(HttpStatus.NOT_FOUND)
                )
                .verify();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
    }

    @Test
    void updateMuseum_whenServerError_shouldThrowException() throws InterruptedException {
        // Arrange
        MuseumUpdateRequest updateRequest = new MuseumUpdateRequest(
                "Test",
                37.6047,
                55.7467,
                null,
                null,
                null,
                null,
                false
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"message\": \"Internal server error\"}"));

        // Act & Assert
        StepVerifier.create(museumApiClient.updateMuseum("some-id", updateRequest))
                .expectErrorMatches(throwable ->
                        throwable instanceof MuseumApiClient.ApiClientException &&
                                ((MuseumApiClient.ApiClientException) throwable).getStatus().equals(HttpStatus.INTERNAL_SERVER_ERROR)
                )
                .verify();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
    }
}
