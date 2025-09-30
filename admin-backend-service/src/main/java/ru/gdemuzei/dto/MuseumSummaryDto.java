package ru.gdemuzei.dto;

public record MuseumSummaryDto(
        String id,
        String name,
        String locality,
        String address,
        boolean verified,
        String website
) {}