package ru.gdemuzei.admin.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Глобальный обработчик исключений для {@link MuseumViewController}.
 * Перехватывает непредвиденные ошибки и отображает стандартизированную страницу ошибки.
 */
@Slf4j
@ControllerAdvice(assignableTypes = MuseumViewController.class)
public class GlobalControllerAdvice {

    /**
     * Обрабатывает любые исключения, не перехваченные локально в контроллере.
     *
     * @param ex       перехваченное исключение.
     * @param exchange текущий веб-обмен для доступа к деталям запроса.
     * @return асинхронный результат рендеринга страницы ошибки.
     */
    @ExceptionHandler(Exception.class)
    public Mono<Rendering> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled exception caught, path: {}, error ID: {}",
                exchange.getRequest().getPath(), errorId, ex);

        return Mono.just(Rendering.view("error")
                .modelAttribute("message", "Произошла непредвиденная ошибка на сервере")
                .modelAttribute("errorId", errorId)
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build());
    }
}
