package ru.gdemuzei.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.geo.GeoResult;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.models.Museum;

@Mapper(componentModel = "spring")
public interface MuseumMapper {

    @Mapping(target = "location", expression = "java(new GeoJsonPoint(request.longitude(), request.latitude()))")
    @Mapping(target = "normalizedOfficialName", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "telegram", target = "telegramChannel")
    @Mapping(target = "adminTags", ignore = true)
    @Mapping(target = "osmData", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Museum toEntity(MuseumCreateRequest request);

    @Mapping(source = "officialName", target = "name")
    @Mapping(source = "location.x", target = "longitude")
    @Mapping(source = "location.y", target = "latitude")
    @Mapping(source = "telegramChannel", target = "telegram")
    MuseumResponse toResponse(Museum museum);

    @Mapping(source = "officialName", target = "name")
    @Mapping(target = "address", expression = "java(calculateDisplayAddress(museum))")
    @Mapping(source = "locality", target = "locality")
    MuseumSummaryDto toSummaryDto(Museum museum);

    @Mapping(target = "adminTags", expression = "java(museum.getAdminTags() != null ? museum.getAdminTags()" +
            " : java.util.Collections.emptySet())")
    @Mapping(source = "location.y", target = "latitude")
    @Mapping(source = "location.x", target = "longitude")
    @Mapping(source = "telegramChannel", target = "telegram")
    MuseumDto toDto(Museum museum);

    @Mapping(source = "content.id", target = "id")
    @Mapping(source = "content.officialName", target = "name")
    @Mapping(target = "address", expression = "java(calculateDisplayAddress(geoResult.getContent()))")
    @Mapping(source = "content.website", target = "website")
    @Mapping(source = "content.telegramChannel", target = "telegramChannel")
    @Mapping(source = "distance.value", target = "distanceKm")
    @Mapping(source = "content.location.x", target = "lon")
    @Mapping(source = "content.location.y", target = "lat")
    MuseumGeoResponse toGeoResponse(GeoResult<Museum> geoResult);

    void updateEntityFromRequest(@MappingTarget Museum museum, MuseumUpdateRequest request);

    // Вспомогательный метод для адреса
    default String calculateDisplayAddress(Museum museum) {
        if (museum == null) {
            return null;
        }
        if (museum.getAddressOverride() != null && !museum.getAddressOverride().isBlank()) {
            return museum.getAddressOverride();
        }
        if (museum.getOsmData() != null) {
            return museum.getOsmData().fullAddress();
        }
        return "";
    }
}