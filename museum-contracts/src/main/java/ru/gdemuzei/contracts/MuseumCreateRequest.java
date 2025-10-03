package ru.gdemuzei.contracts;

import jakarta.validation.constraints.*;

public record MuseumCreateRequest(
        @NotBlank(message = "Название музея не может быть пустым")
        @Size(max = 255, message = "Название должно содержать до 255 символов")
        String officialName,

        @NotNull(message = "Долгота обязательна")
        @DecimalMin(value = "-180.0", message = "Долгота не может быть меньше -180")
        @DecimalMax(value = "180.0", message = "Долгота не может быть больше 180")
        Double longitude,

        @NotNull(message = "Широта обязательна")
        @DecimalMin(value = "-90.0", message = "Широта не может быть меньше -90")
        @DecimalMax(value = "90.0", message = "Широта не может быть больше 90")
        Double latitude,

        @Pattern(regexp = "^@?[a-zA-Z][a-zA-Z0-9_]{3,30}[a-zA-Z0-9]$",
                message = "Некорректный формат Telegram-канала")
        String telegram,

        @Pattern(regexp = "^(?:https?://)?(?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9-]*(?:\\.[a-zA-Z0-9][a-zA-Z0-9-]*)+(?::\\d{2,5})?(?:/[^\\s]*)?$",
                message = "Некорректный формат сайта")
        @Size(max = 2048, message = "Адрес сайта не должен превышать 2048 символов")
        String website,

        @Size(max = 500, message = "Адрес не должен превышать 500 символов")
        String addressOverride,

        @Size(max = 100, message = "Название населенного пункта не должно превышать 100 символов")
        String locality
) {
        public static MuseumCreateRequest empty() {
                return new MuseumCreateRequest(null, null, null, null, null, null, null);
        }
}