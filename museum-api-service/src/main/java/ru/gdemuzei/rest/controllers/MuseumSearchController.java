package ru.gdemuzei.rest.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.services.MuseumService;

/**
 * Контроллер для выполнения сложных поисковых запросов.
 * Предоставляет разные эндпоинты для публичных и административных клиентов.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MuseumSearchController {

    private final MuseumService museumService;

    /**
     * GET /api/admin/v1/museums/search — возвращает поток ближайших музеев.
     *
     * <p>Обязательные query-параметры: {@code lat}, {@code lon}.
     * Необязательные: {@code offset}, {@code limit}.</p>
     *
     * @param lat    широта
     * @param lon    долгота
     * @param offset смещение
     * @param limit  количество
     * @return 200 OK и NDJSON-стрим {@link MuseumGeoResponse}.
     */
    @GetMapping(path = "${api.paths.public.search}",
            produces = MediaType.APPLICATION_NDJSON_VALUE) // Важно: NDJSON для стриминга
    public Flux<MuseumGeoResponse> searchNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Telegramm search for nearby museums: lat={}, lon={}, offset={}, limit={}", lat, lon, offset, limit);
        return museumService.findNearbyWithOffset(lat, lon, offset, limit);
    }

}
