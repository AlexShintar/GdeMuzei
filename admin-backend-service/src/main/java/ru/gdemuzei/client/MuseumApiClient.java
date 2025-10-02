package ru.gdemuzei.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.gdemuzei.dto.ErrorResponse;
import ru.gdemuzei.dto.MuseumCreateRequest;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.MuseumSummaryDto;
import ru.gdemuzei.dto.MuseumUpdateRequest;
import ru.gdemuzei.services.RestPageImpl;

@Service
@RequiredArgsConstructor
public class MuseumApiClient {

    private final WebClient webClient;

    public Mono<Page<MuseumSummaryDto>> getMuseumsPaginated(int page, int size) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/v2/museums")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RestPageImpl<MuseumSummaryDto>>() {
                })
                .map(restPage -> restPage);
    }

    public Mono<MuseumDto> getMuseumById(String id) {
        return this.webClient.get()
                .uri("/api/public/v2/museums/{id}", id)
                .retrieve()
                .bodyToMono(MuseumDto.class)
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.empty());
    }

    public Mono<MuseumDto> createMuseum(MuseumCreateRequest request) {
        return webClient.post()
                .uri("/api/admin/v2/museums")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(MuseumDto.class);
                    }
                    return readError(resp);
                });
    }

    public Mono<MuseumDto> updateMuseum(String id, MuseumUpdateRequest request) {
        return webClient.put()
                .uri("/api/admin/v2/museums/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(MuseumDto.class);
                    }
                    return readError(resp);
                });
    }

    /**
     * Унифицированное чтение тела ошибки сервера и проброс как исключения клиента.
     */
    private <T> Mono<T> readError(ClientResponse resp) {
        return resp.bodyToMono(ErrorResponse.class)
                .defaultIfEmpty(new ErrorResponse("Unknown error"))
                .flatMap(er -> Mono.error(new ApiClientException(resp.statusCode(), er.message())));
    }

    /**
     * Исключение клиента с кодом статуса.
     */
    @Getter
    public static class ApiClientException extends RuntimeException {
        private final HttpStatusCode status;

        public ApiClientException(HttpStatusCode status, String message) {
            super(message);
            this.status = status;
        }
    }
}