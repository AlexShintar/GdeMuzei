package ru.gdemuzei.rest.handlers;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.exceptions.EntityNotFoundException;
import ru.gdemuzei.services.MuseumService;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MuseumHandler {

    private final MuseumService museumService;
    private final Validator validator;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final int DEFAULT_SIZE = 20;
    private static final int DEFAULT_PAGE = 0;

    /**
     * Возвращает страницу кратких сведений о музеях.
     *
     * <p>Query-параметры: {@code page} (0..), {@code size} (&gt;0). Значения по умолчанию: 0 и 20.</p>
     *
     * @param request HTTP-запрос
     * @return 200 OK и JSON-страница {@link Page}&lt;{@link MuseumSummaryDto}&gt;
     */
    public Mono<ServerResponse> getAllMuseumsPaginated(ServerRequest request) {
        int page = qpInt(request, "page", DEFAULT_PAGE);
        int size = qpInt(request, "size", DEFAULT_SIZE);

        Mono<List<MuseumSummaryDto>> content = museumService.findAllPaginated(page, size).collectList();
        Mono<Long> total = museumService.countAll();

        return Mono.zip(content, total)
                .flatMap(t -> {
                    Page<MuseumSummaryDto> result = new PageImpl<>(t.getT1(), PageRequest.of(page, size), t.getT2());
                    return ServerResponse.ok().contentType(JSON).bodyValue(result);
                });
    }

    /**
     * Возвращает подробную информацию о музее по идентификатору.
     *
     * @param request HTTP-запрос (путь содержит {@code {id}})
     * @return 200 OK и {@link MuseumDto}; если не найден — пробрасывает исключение для 404
     */
    public Mono<ServerResponse> getMuseumById(ServerRequest request) {
        String id = request.pathVariable("id");
        return museumService.findById(id)
                .flatMap(dto -> ServerResponse.ok().contentType(JSON).bodyValue(dto))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Museum not found: " + id)));
    }

    /**
     * Возвращает поток ближайших музеев.
     *
     * <p>Обязательные query-параметры: {@code lat}, {@code lon}. Необязательные: {@code offset}, {@code limit}.</p>
     *
     * @param request HTTP-запрос
     * @return 200 OK и NDJSON-стрим {@link MuseumGeoResponse}
     */
    public Mono<ServerResponse> searchNearby(ServerRequest request) {
        double lat = qpDoubleRequired(request, "lat");
        double lon = qpDoubleRequired(request, "lon");
        int offset = qpInt(request, "offset", 0);
        int limit = qpInt(request, "limit", 10);

        Flux<MuseumGeoResponse> flux = museumService.findNearbyWithOffset(lat, lon, offset, limit);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(flux, MuseumGeoResponse.class);
    }

    /**
     * Создаёт новый музей.
     *
     * <p>Тело запроса: {@link MuseumCreateRequest}. Выполняется Bean Validation.</p>
     *
     * @param request HTTP-запрос
     * @return 201 Created, JSON тела и заголовок Location; при валидации — 400; при конфликте имени — 409
     */
    public Mono<ServerResponse> createMuseum(ServerRequest request) {
        return readAndValidate(request, MuseumCreateRequest.class)
                .flatMap(museumService::create)
                .flatMap(resp -> ServerResponse
                        .created(buildLocation(request, resp.id()))
                        .contentType(JSON)
                        .bodyValue(resp));
    }

    /**
     * Обновляет существующий музей.
     *
     * <p>Тело запроса: {@link MuseumUpdateRequest}. Выполняется Bean Validation.</p>
     *
     * @param request HTTP-запрос (путь содержит {@code {id}})
     * @return 200 OK и обновлённый JSON; при отсутствии — 404; при конфликте имени — 409; при валидации — 400
     */
    public Mono<ServerResponse> updateMuseum(ServerRequest request) {
        String id = request.pathVariable("id");
        return readAndValidate(request, MuseumUpdateRequest.class)
                .flatMap(dto -> museumService.update(id, dto))
                .flatMap(resp -> ServerResponse.ok().contentType(JSON).bodyValue(resp));
    }

    /**
     * Читает тело запроса и валидирует DTO через Bean Validation.
     * При наличии нарушений выбрасывает {@link IllegalArgumentException} с деталями (перехватывается фильтром роутера).
     *
     * @param req   HTTP-запрос
     * @param clazz целевой класс DTO
     * @param <T>   тип DTO
     * @return моно с валидным DTO
     */
    private <T> Mono<T> readAndValidate(ServerRequest req, Class<T> clazz) {
        return req.bodyToMono(clazz).flatMap(dto -> {
            var violations = validator.validate(dto);
            if (violations.isEmpty()) return Mono.just(dto);
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return Mono.error(new IllegalArgumentException("Validation failed: " + msg));
        });
    }

    /**
     * Формирует корректный URI ресурса для заголовка Location.
     * Удаляет query-строку и добавляет {@code id} как сегмент пути.
     *
     * @param req HTTP-запрос
     * @param id  идентификатор созданного ресурса
     * @return абсолютный или базовый URI ресурса
     */
    private static URI buildLocation(ServerRequest req, String id) {
        return req.uriBuilder()
                .replaceQuery(null)
                .pathSegment(id)
                .build();
    }

    /**
     * Читает целочисленный query-параметр, возвращая значение по умолчанию при отсутствии.
     *
     * @param req  HTTP-запрос
     * @param name имя параметра
     * @param def  значение по умолчанию
     * @return число из параметра или {@code def}
     */
    private static int qpInt(ServerRequest req, String name, int def) {
        return req.queryParam(name).map(Integer::parseInt).orElse(def);
    }

    /**
     * Читает обязательный числовой (double) query-параметр.
     * При отсутствии или ошибке парсинга выбрасывает {@link IllegalArgumentException}.
     *
     * @param req  HTTP-запрос
     * @param name имя параметра
     * @return значение параметра как double
     */
    private static double qpDoubleRequired(ServerRequest req, String name) {
        return req.queryParam(name)
                .map(Double::parseDouble)
                .orElseThrow(() -> new IllegalArgumentException(name + " is required"));
    }
}
