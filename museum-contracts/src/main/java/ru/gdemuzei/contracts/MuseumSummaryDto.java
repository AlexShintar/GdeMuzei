package ru.gdemuzei.contracts;

public record MuseumSummaryDto(
        String id,
        String name,
        String address,
        String website,
        String telegramChannel,
        Double distanceKm,
        double  lat,
        double  lon
) {}