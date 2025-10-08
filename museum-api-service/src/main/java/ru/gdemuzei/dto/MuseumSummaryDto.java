package ru.gdemuzei.dto;

public record MuseumSummaryDto(
        String id,
        String name,
        String locality, // Название города/населенного пункта
        String address, // Полный адрес из OSM или переопределенный
        boolean verified, // Статус верификации
        String website
) {}