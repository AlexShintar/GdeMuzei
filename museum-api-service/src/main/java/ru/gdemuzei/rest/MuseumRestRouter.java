package ru.gdemuzei.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import ru.gdemuzei.exceptions.EntityNotFoundException;
import ru.gdemuzei.dto.ErrorResponse;
import ru.gdemuzei.rest.handlers.MuseumHandler;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class MuseumRestRouter {
    /**
     * Регистрирует маршруты ресурса «музеи» и подключает фильтр ошибок.
     *
     * <p>Маршруты:
     * <ul>
     *   <li>GET /api/v1/museums — постраничный список</li>
     *   <li>GET /api/v1/museums/{id} — получение по идентификатору</li>
     *   <li>GET /api/v1/museums/search — поиск ближайших (NDJSON)</li>
     *   <li>POST /api/v1/museums — создание</li>
     *   <li>PUT /api/v1/museums/{id} — обновление</li>
     * </ul>
     * Фильтр возвращает 400/404/409 с JSON-телом {@link ErrorResponse} для соответствующих исключений.</p>
     *
     * @param handler обработчик запросов
     * @return функциональный роутер WebFlux
     */
    @Bean
    public RouterFunction<ServerResponse> museumRoutes(MuseumHandler handler) {
        return RouterFunctions
                .route(GET("/api/v1/museums"), handler::getAllMuseumsPaginated)
                .andRoute(GET("/api/v1/museums/search"), handler::searchNearby)
                .andRoute(GET("/api/v1/museums/{id}"), handler::getMuseumById)
                .andRoute(POST("/api/v1/museums"), handler::createMuseum)
                .andRoute(PUT("/api/v1/museums/{id}"), handler::updateMuseum)
                .filter((req, next) -> next.handle(req)
                        .onErrorResume(IllegalArgumentException.class, e ->
                                ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new ErrorResponse(e.getMessage())))
                        .onErrorResume(NumberFormatException.class, e ->
                                ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new ErrorResponse("Invalid query parameter.")))
                        .onErrorResume(EntityNotFoundException.class, e ->
                                ServerResponse.status(HttpStatus.NOT_FOUND)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new ErrorResponse(e.getMessage())))
                        .onErrorResume(org.springframework.dao.DataIntegrityViolationException.class, e ->
                                ServerResponse.status(HttpStatus.CONFLICT)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(new ErrorResponse("Duplicate resource.")))
                );
    }
}