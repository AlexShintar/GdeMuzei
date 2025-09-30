package ru.gdemuzei.bot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import ru.gdemuzei.bot.config.MuseumApiConfig;
import ru.gdemuzei.bot.dto.MuseumSummaryDto;

@Service
@RequiredArgsConstructor
public class MuseumApiClient {

    private final WebClient webClient;

    private final MuseumApiConfig apiConfig;

    public Flux<MuseumSummaryDto> search(double lat, double lon, int offset, int limit) {
        return webClient.get()
                .uri(uri -> uri
                        .path(apiConfig.searchPath())
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToFlux(MuseumSummaryDto.class);
    }
}
