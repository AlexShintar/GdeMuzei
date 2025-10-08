package ru.gdemuzei.rest;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import ru.gdemuzei.dto.ErrorResponse;
import ru.gdemuzei.exceptions.EntityNotFoundException;
import ru.gdemuzei.exceptions.ForbiddenException;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для REST-контроллеров.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 400 - Некорректные параметры запроса (ручные проверки, парсинг)
    @ExceptionHandler({IllegalArgumentException.class, NumberFormatException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 400 - Ошибки валидации тела DTO (@Valid)
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleDtoValidation(WebExchangeBindException ex) {
        String errors = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation failed: " + errors));
    }

    // 400 - Ошибки валидации параметров запроса (@RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation failed: " + errors));
    }

    // 400 - Ошибки парсинга/декодирования тела запроса
    @ExceptionHandler({ServerWebInputException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleInput(Exception ex) {
        log.warn("Invalid request format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid request format"));
    }

    // 404 - Ресурс не найден
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 403 - Доступ запрещен (Spring Security)
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Forbidden"));
    }

    // 401 - Ошибка аутентификации
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized"));
    }

    // 409 - Конфликт данных (дубликат уникального поля)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Duplicate resource."));
    }

    // Обработчик для ResponseStatusException (позволяет программно задавать статус)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        String message = ex.getReason() != null ? ex.getReason() : code.toString();

        if (code.is5xxServerError()) {
            String errorId = UUID.randomUUID().toString();
            log.error("Server error triggered via RSE [errorId={}]: {}", errorId, message, ex);
            return ResponseEntity.status(code)
                    .body(new ErrorResponse("Internal server error. Error ID: " + errorId));
        }

        log.warn("Request handled with status {}: {}", code.value(), message);
        return ResponseEntity.status(code).body(new ErrorResponse(message));
    }

    // 500 - "Catch-all" для всех остальных, непредвиденных ошибок
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error [errorId={}]", errorId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error. Error ID: " + errorId));
    }
}
