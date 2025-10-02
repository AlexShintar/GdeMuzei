package ru.gdemuzei.dto;

public record MuseumResponse(
        String id,
        String name,
        String telegram,
        double longitude,
        double latitude
) {}