package ru.gdemuzei.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO для ответа об ошибке.
 * @param message Основное сообщение об ошибке.
 * @param errorId Уникальный идентификатор для внутренних ошибок сервера (может быть null).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        String errorId
) {
    /**
     * Вспомогательный конструктор для ошибок, не требующих errorId.
     * @param message Сообщение об ошибке.
     */
    public ErrorResponse(String message) {
        this(message, null);
    }
}