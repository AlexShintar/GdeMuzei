package ru.gdemuzei.rest.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.MuseumResponse;
import ru.gdemuzei.services.MuseumService;

/**
 * Контроллер для публичного доступа к информации о музеях.
 * Предоставляет только чтение отдельных музеев по ID.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicMuseumController {

    private final MuseumService museumService;

    /**
     * GET /api/public/v1/museums/{id} — возвращает подробную информацию о музее по ID.
     *
     * <p>Доступен всем пользователям. Возвращает только активные (не удалённые) музеи.</p>
     *
     * @param id идентификатор музея.
     * @return 200 OK и полные данные о музее {@link MuseumResponse}.
     * @throws ru.gdemuzei.exceptions.EntityNotFoundException если музей не найден или удалён.
     */
    @GetMapping(path = "${api.paths.public.museums}/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MuseumDto> getMuseumById(@PathVariable String id) {
        log.info("Public request for museum id={}", id);
        return museumService.findById(id);
    }
}
