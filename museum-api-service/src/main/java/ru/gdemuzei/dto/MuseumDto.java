package ru.gdemuzei.dto;

import ru.gdemuzei.models.OsmData;

import java.util.Set;

public record MuseumDto(
        String id,
        String officialName,
        String locality,
        String addressOverride,
        String website,
        String telegram,
        Set<String> adminTags,
        Boolean verified,
        OsmData osmData,
        Double latitude,
        Double longitude
) {
}