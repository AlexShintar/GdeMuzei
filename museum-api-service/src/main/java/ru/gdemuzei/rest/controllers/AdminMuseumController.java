package ru.gdemuzei.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.services.MuseumService;
import java.net.URI;

/**
 * Контроллер для выполнения административных операций над музеями.
 * Все эндпоинты требуют роль 'ADMIN'.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
//@PreAuthorize("hasRole('ADMIN')")
public class AdminMuseumController {

    private final MuseumService museumService;

    @Value("${api.paths.public.museums}")
    public String PUBLIC_MUSEUMS;

    /**
     * GET /api/admin/v1/museums — возвращает пагинированный список всех музеев.
     *
     * <p>Используется для простого отображения в админ-панели без сложных фильтров.
     * Показывает как активные, так и удалённые музеи.</p>
     *
     * @param page номер страницы (0-based).
     * @param size размер страницы.
     * @return 200 OK и страница с полными данными о музеях {@link Page}&lt;{@link MuseumResponse}&gt;.
     */
    @GetMapping(path = "${api.paths.admin.museums}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Page<MuseumSummaryDto>> getAllMuseumsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin request for all museums, page={}, size={}", page, size);
        return museumService.findAllForAdmin(PageRequest.of(page, size));
    }

    /**
     * POST /api/admin/v1/museums — создаёт новый музей.
     *
     * @param request DTO с данными для создания музея.
     * @return 201 Created с телом созданного ресурса и заголовком Location.
     */
    @PostMapping(path = "${api.paths.admin.museums}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<MuseumDto>> createMuseum(
            @Valid @RequestBody MuseumCreateRequest request) {
        log.info("Creating museum: {}", request.officialName());
        return museumService.create(request)
                .map(created -> ResponseEntity
                        .created(URI.create(PUBLIC_MUSEUMS + created.id()))
                        .body(created));
    }

    /**
     * PUT /api/admin/v1/museums/{id} — обновляет существующий музей.
     *
     * <p>После успешного обновления инвалидируется кэш summary в Redis.</p>
     *
     * @param id      идентификатор музея для обновления.
     * @param request DTO с данными для обновления.
     * @return 200 OK с обновлённым телом ресурса.
     */
    @PutMapping(path = "${api.paths.admin.museums}/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MuseumDto>> updateMuseum(
            @PathVariable String id,
            @Valid @RequestBody MuseumUpdateRequest request) {
        log.info("Updating museum id={}", id);
        return museumService.update(id, request)
                .map(ResponseEntity::ok);
    }

    /**
     * DELETE /api/admin/v1/museums/{id} — помечает музей как удалённый (soft delete).
     *
     * <p>Фактическое удаление из базы данных не происходит. Устанавливается флаг deleted=true
     * и музей исчезает из публичного API.</p>
     *
     * <p>После успешного удаления инвалидируется кэш summary в Redis.</p>
     *
     * @param id идентификатор музея для удаления.
     * @return 204 No Content.
     */
    @DeleteMapping("${api.paths.admin.museums}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMuseum(@PathVariable String id) {
        log.info("Marking museum as deleted, id={}", id);
        return museumService.markAsDeleted(id);
    }
}