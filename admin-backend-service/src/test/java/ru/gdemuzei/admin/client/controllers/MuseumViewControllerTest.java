package ru.gdemuzei.admin.client.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import ru.gdemuzei.admin.client.MuseumApiClient;
import ru.gdemuzei.admin.controllers.GlobalControllerAdvice;
import ru.gdemuzei.admin.controllers.MuseumViewController;
import ru.gdemuzei.admin.controllers.PageController;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.admin.dto.MuseumSummaryDto;
import ru.gdemuzei.admin.dto.MuseumUpdateRequest;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromFormData;

/**
 * Unit-тесты для {@link MuseumViewController}.
 * Проверяют логику контроллера без рендеринга шаблонов.
 */
@WebFluxTest(controllers = {MuseumViewController.class, PageController.class})
@Import(GlobalControllerAdvice.class)
class MuseumViewControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MuseumApiClient museumApiClient;

    @Test
    void showMuseumsPage_shouldReturnOkStatus() {
        // Given
        var page = new PageImpl<>(
                List.of(new MuseumSummaryDto("1", "Эрмитаж", "СПб", "Адрес", true, "https://site.ru")),
                PageRequest.of(0, 20),
                1
        );
        when(museumApiClient.getMuseumsPaginated(0, 20)).thenReturn(Mono.just(page));

        // When & Then
        webTestClient.get().uri("/museums?page=0&size=20")
                .exchange()
                .expectStatus().isOk();

        verify(museumApiClient).getMuseumsPaginated(0, 20);
    }

    @Test
    void showMuseumsPage_withDefaultParams_shouldUseDefaults() {
        // Given
        var emptyPage = new PageImpl<MuseumSummaryDto>(
                Collections.emptyList(),
                PageRequest.of(0, 20),
                0
        );
        when(museumApiClient.getMuseumsPaginated(0, 20)).thenReturn(Mono.just(emptyPage));

        // When & Then
        webTestClient.get().uri("/museums")
                .exchange()
                .expectStatus().isOk();

        verify(museumApiClient).getMuseumsPaginated(0, 20);
    }

    @Test
    void showMuseumEditPage_whenFound_shouldReturnOkStatus() {
        // Given
        var dto = new MuseumDto(
                "id1",
                "Третьяковская галерея",
                "Москва",
                "Лаврушинский переулок, 10",
                "https://tretyakov.ru",
                "@tretyakov",
                Collections.emptySet(),
                true,
                null,
                55.7414,
                37.6207
        );
        when(museumApiClient.getMuseumById("id1")).thenReturn(Mono.just(dto));

        // When & Then
        webTestClient.get().uri("/museums/id1?fromPage=1&fromSize=10")
                .exchange()
                .expectStatus().isOk();

        verify(museumApiClient).getMuseumById("id1");
    }

    @Test
    void showMuseumEditPage_whenNotFound_shouldReturnOkStatusWithNotFoundView() {
        // Given
        when(museumApiClient.getMuseumById("nonexistent")).thenReturn(Mono.empty());

        // When & Then
        webTestClient.get().uri("/museums/nonexistent")
                .exchange()
                .expectStatus().isOk();

        verify(museumApiClient).getMuseumById("nonexistent");
    }

    @Test
    void showCreateMuseumForm_shouldReturnOkStatus() {
        // When & Then
        webTestClient.get().uri("/museums/create")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void createMuseum_whenValid_shouldRedirectToList() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Пушкинский музей");
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");
        formData.add("locality", "Москва");

        var createdMuseum = new MuseumDto(
                "new-id",
                "Пушкинский музей",
                "Москва",
                null,
                null,
                null,
                null,
                false,
                null,
                55.7467,
                37.6047
        );
        when(museumApiClient.createMuseum(any(MuseumCreateRequest.class)))
                .thenReturn(Mono.just(createdMuseum));

        // When & Then
        webTestClient.post().uri("/museums")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/museums");

        verify(museumApiClient).createMuseum(any(MuseumCreateRequest.class));
    }

    @Test
    void createMuseum_whenInvalidEmptyName_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "");
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");

        // When & Then
        webTestClient.post().uri("/museums")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createMuseum_whenInvalidCoordinates_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Test Museum");
        formData.add("longitude", "200.0");
        formData.add("latitude", "100.0");

        // When & Then
        webTestClient.post().uri("/museums")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createMuseum_whenMissingRequiredFields_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Test Museum");

        // When & Then
        webTestClient.post().uri("/museums")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateMuseum_whenValid_shouldRedirectWithPageParams() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Обновленное название");
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");
        formData.add("locality", "Москва");
        formData.add("verified", "true");

        var updatedMuseum = new MuseumDto(
                "id1",
                "Обновленное название",
                "Москва",
                null,
                null,
                null,
                null,
                true,
                null,
                55.7467,
                37.6047
        );
        when(museumApiClient.updateMuseum(eq("id1"), any(MuseumUpdateRequest.class)))
                .thenReturn(Mono.just(updatedMuseum));

        // When & Then
        webTestClient.post().uri("/museums/id1?fromPage=2&fromSize=10")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/museums?page=2&size=10");

        verify(museumApiClient).updateMuseum(eq("id1"), any(MuseumUpdateRequest.class));
    }

    @Test
    void updateMuseum_whenInvalidEmptyName_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", ""); // Пустое имя
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");

        // When & Then
        webTestClient.post().uri("/museums/id1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateMuseum_whenInvalidCoordinatesOutOfRange_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Test Museum");
        formData.add("longitude", "200.0"); // > 180
        formData.add("latitude", "100.0");  // > 90

        // When & Then
        webTestClient.post().uri("/museums/id1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateMuseum_whenInvalidWebsite_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Test Museum");
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");
        formData.add("website", "not-a-url"); // Невалидный URL

        // When & Then
        webTestClient.post().uri("/museums/id1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateMuseum_whenInvalidTelegram_shouldReturn422() {
        // Given
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("officialName", "Test Museum");
        formData.add("longitude", "37.6047");
        formData.add("latitude", "55.7467");
        formData.add("telegram", "invalid telegram handle with spaces"); // Невалидный формат

        // When & Then
        webTestClient.post().uri("/museums/id1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(fromFormData(formData))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void mainPageRedirect_shouldRedirectToMuseums() {
        // When & Then
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/museums");
    }
}