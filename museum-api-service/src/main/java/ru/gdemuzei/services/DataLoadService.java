package ru.gdemuzei.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.dto.NominatimResponse;
import ru.gdemuzei.models.OsmData;
import ru.gdemuzei.util.AddressFormatter;
import ru.gdemuzei.util.MuseumCsvParser;
import ru.gdemuzei.client.NominatimClient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataLoadService {

    private final MuseumService museumService;

    private final MuseumCsvParser museumCsvParser;

    private final NominatimClient nominatimClient;

    @Value("${osm.enrich.delay-between:3s}")
    Duration delayBetween;

    @Value("${osm.enrich.retry-delay:7s}")
    Duration retryDelay;

    @Value("${osm.enrich.retry-attempts:2}")
    int retryAttempts;

    /**
     * Загружает данные о музеях из CSV-файла в базу данных.
     * Пропускает дубликаты по нормализованному имени.
     *
     * @param path Путь к CSV-файлу.
     * @return Mono, содержащее количество успешно добавленных музеев.
     */
    public Mono<Integer> loadMuseumsFromCsv(Path path) {

        List<MuseumCreateRequest> dtos = museumCsvParser.parseMuseums(path);
        return Flux.fromIterable(dtos)
                .concatMap(dto -> museumService.create(dto)
                        .doOnSuccess(v -> log.info("Created museum: {}", dto.officialName()))
                        .map(m -> 1)
                        .onErrorResume(DataIntegrityViolationException.class, e -> {
                            log.warn("Skipping duplicate museum: {}", dto.officialName());
                            return Mono.just(0);
                        })
                )
                .reduce(0, Integer::sum);
    }

    /**
     * Обогащает данные для музеев, у которых отсутствует информация из OSM.
     * Последовательно обрабатывает музеи, делает запросы к Nominatim API с задержками
     * и повторными попытками в случае сбоев.
     * Прекращает работу при получении ответа о превышении лимита запросов (429/403).
     *
     * @return Mono<Void>, который завершается, когда процесс обогащения закончен.
     */
    public Mono<Void> enrichUnverifiedMuseumsFromOsm() {
        return museumService.findMuseumsWithoutOsmData()
                .delayElements(delayBetween)
                .concatMap(museum -> {
                    log.info("Processing museum: {}", museum.getOfficialName());
                    GeoJsonPoint location = museum.getLocation();

                    return nominatimClient.fetchInfo(location.getY(), location.getX())
                            .map(this::convertToOsmData)
                            .flatMap(osmData -> museumService.updateOsmData(museum.getId(), osmData))
                            .retryWhen(reactor.util.retry.Retry
                                    .fixedDelay(retryAttempts, retryDelay)
                                    .filter(ex -> !isRateLimitOrBan(ex)))
                            .onErrorResume(e -> {
                                if (isRateLimitOrBan(e)) {
                                    log.error("OSM rate limit/ban detected. Stopping enrichment. Err={}", e.toString());
                                    return Mono.error(e);
                                } else {
                                    log.warn("Failed to enrich museum '{}', skipping. Err={}",
                                            museum.getOfficialName(), e.toString());
                                    return Mono.empty();
                                }
                            });
                })
                .then();
    }

    /**
     * Преобразует ответ Nominatim в доменную структуру OSM-данных.
     * Пустые/отсутствующие поля аккуратно заменяются {@code null} или пустыми строками.
     *
     * @param osmResponse ответ Nominatim
     * @return агрегированные данные для сохранения в музей
     */
    private OsmData convertToOsmData(NominatimResponse osmResponse) {
        Map<String, String> address = osmResponse.address() != null
                ? osmResponse.address()
                : Collections.emptyMap();
        Map<String, String> tags = osmResponse.extratags() != null
                ? osmResponse.extratags()
                : Collections.emptyMap();
        String city = AddressFormatter.chooseLocality(address);
        String formattedAddress = AddressFormatter.buildAddress(address);
        String fullAddress = !formattedAddress.isBlank()
                ? formattedAddress : osmResponse.displayName();
        String openingHours = tags.get("opening_hours");
        String phone = tags.getOrDefault("phone", tags.get("contact:phone"));
        String email = tags.getOrDefault("email", tags.get("contact:email"));
        String website = tags.get("website");
        String wikipedia = tags.get("wikipedia");
        String wikidata = tags.get("wikidata");
        String heritage = tags.get("heritage");
        String startDate = tags.get("start_date");
        return new OsmData(city, fullAddress, openingHours,
                phone, email, website, wikipedia, wikidata, heritage, startDate
        );
    }

    /**
     * Определяет, соответствует ли ошибка ограничению по частоте запросов или временному бану.
     * Учитываются коды HTTP 429 и 403, а также диагностические сообщения.
     *
     * @param e исходная ошибка
     * @return {@code true}, если обнаружен лимит/бан, иначе {@code false}
     */
    private boolean isRateLimitOrBan(Throwable e) {
        Throwable t = Exceptions.unwrap(e);
        if (t instanceof WebClientResponseException wcre) {
            HttpStatusCode sc = wcre.getStatusCode();
            int code = sc.value();
            return code == HttpStatus.TOO_MANY_REQUESTS.value()
                    || code == HttpStatus.FORBIDDEN.value();
        }
        String msg = String.valueOf(t.getMessage()).toLowerCase();
        return msg.contains("429") || msg.contains("too many requests");
    }
}
