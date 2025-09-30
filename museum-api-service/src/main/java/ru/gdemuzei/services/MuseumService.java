package ru.gdemuzei.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.models.Museum;
import ru.gdemuzei.models.OsmData;

public interface MuseumService {

    Flux<MuseumResponse> findAll();

    Flux<MuseumSummaryDto> findAllPaginated(int page, int size);

    Mono<Long> countAll();

    Mono<MuseumDto> findById(String id);

    Mono<MuseumResponse> create(MuseumCreateRequest request);

    Flux<Museum> findMuseumsWithoutOsmData();

    Mono<Museum> updateOsmData(String museumId, OsmData osmData);

    Flux<MuseumGeoResponse> findNearbyWithOffset(double lat, double lon, int offset, int limit);

    Mono<MuseumResponse> update(String id, MuseumUpdateRequest request);
}
