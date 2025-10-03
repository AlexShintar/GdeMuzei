package ru.gdemuzei.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.contracts.MuseumCreateRequest;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.models.Museum;
import ru.gdemuzei.models.OsmData;

public interface MuseumService {

    Mono<Page<MuseumSummaryDto>> findAllForAdmin(PageRequest pageRequest);

    Mono<Long> countAll();

    Mono<MuseumDto> findById(String id);

    Mono<MuseumDto> create(MuseumCreateRequest request);

    Flux<Museum> findMuseumsWithoutOsmData();

    Mono<Museum> updateOsmData(String museumId, OsmData osmData);

    Flux<MuseumGeoResponse> findNearbyWithOffset(double lat, double lon, int offset, int limit);

    Mono<MuseumDto> update(String id, MuseumUpdateRequest request);

    Mono<Void> markAsDeleted(String id);
}
