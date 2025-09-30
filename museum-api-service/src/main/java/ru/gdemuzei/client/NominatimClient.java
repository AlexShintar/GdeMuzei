package ru.gdemuzei.client;

//import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.gdemuzei.dto.NominatimResponse;

//@Profile("shell")
@Component
@RequiredArgsConstructor
@Slf4j
public class NominatimClient {

    private final WebClient webClient;

    public Mono<NominatimResponse> fetchInfo(double latitude, double longitude) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reverse")
                        .queryParam("format", "json")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("addressdetails", 1)
                        .queryParam("extratags", 1)
                        .build())
                .retrieve()
                .bodyToMono(NominatimResponse.class)
                .onErrorResume(e -> {
                    log.error("Failed to fetch data from Nominatim for lat={}, lon={}: {}",
                            latitude, longitude, e.getMessage());
                    return Mono.empty();
                });
    }
}
