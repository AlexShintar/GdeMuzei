package ru.gdemuzei.dto;

public record MuseumGeoResponse(
        String id,
        String name,
        String address,
        String website,
        String telegramChannel,
        Double distanceKm,
        double  lat,
        double  lon
) {}