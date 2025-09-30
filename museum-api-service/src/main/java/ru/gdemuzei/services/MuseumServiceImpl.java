package ru.gdemuzei.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.exceptions.EntityNotFoundException;
import ru.gdemuzei.mapper.MuseumMapper;
import ru.gdemuzei.models.Museum;
import ru.gdemuzei.models.OsmData;
import ru.gdemuzei.repositories.MuseumRepository;
import ru.gdemuzei.util.NameNormalizer;

@RequiredArgsConstructor
@Service
public class MuseumServiceImpl implements MuseumService {
    private final MuseumMapper museumMapper;

    private final MuseumRepository museumRepository;

    private final ReactiveMongoTemplate mongoTemplate;

    @Value("${application.nearby.radius-km:10.0}")
    private double nearbyRadiusKm;

    /**
     * Возвращает поток музеев
     */
    @Override
    public Flux<MuseumResponse> findAll() {
        return museumRepository.findAll()
                .map(museumMapper::toResponse);
    }

    /**
     * Постраничный вывод краткой информации о музеях.
     *
     * @param page номер страницы (0-based)
     * @param size размер страницы
     */
    @Override
    public Flux<MuseumSummaryDto> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return museumRepository.findAllBy(pageable)
                .map(museumMapper::toSummaryDto);
    }

    /** Общее количество музеев. */
    @Override
    public Mono<Long> countAll() {
        return museumRepository.count();
    }

    /** Поиск музея по идентификатору. */
    @Override
    public Mono<MuseumDto> findById(String id) {
        return museumRepository.findById(id)
                .map(museumMapper::toDto);
    }

    /**
     * Создание нового музея.
     * Название нормализуется; дубликаты по нормализованному названию запрещены.
     */
    @Override
    public Mono<MuseumResponse> create(MuseumCreateRequest request) {

        String normalizedName = NameNormalizer.normalize(request.officialName());

        return museumRepository.existsByNormalizedOfficialName(normalizedName)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DataIntegrityViolationException("Museum with name '"
                                + request.officialName() + "' already exists."));
                    }
                    Museum museum = museumMapper.toEntity(request);
                    museum.setVerified(false);
                    museum.setNormalizedOfficialName(normalizedName);
                    return museumRepository.save(museum)
                            .map(museumMapper::toResponse);
                });
    }

    /** Музеи, у которых отсутствуют OSM-данные. */
    @Override
    public Flux<Museum> findMuseumsWithoutOsmData() {
        return museumRepository.findByOsmDataIsNull();
    }

    /**
     * Обновление OSM-данных музея.
     * Существующие значения сайта/населённого пункта не перезаписываются.
     */
    @Override
    public Mono<Museum> updateOsmData(String museumId, OsmData osmData) {
        return museumRepository.findById(museumId)
                .flatMap(museum -> {
                    museum.setOsmData(osmData);

                    if (museum.getWebsite() == null || museum.getWebsite().isEmpty()) {
                        museum.setWebsite(osmData.website());
                    }
                    if (museum.getLocality() == null || museum.getLocality().isEmpty()) {
                        museum.setLocality(osmData.locality());
                    }

                    return museumRepository.save(museum);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Museum not found with id " + museumId)));
    }

    /**
     * Поиск ближайших музеев в радиусе.
     * Результат отсортирован по расстоянию, поддерживает смещение и лимит.
     */
    @Override
    public Flux<MuseumGeoResponse> findNearbyWithOffset(double lat, double lon, int offset, int limit) {
        Point userLocation = new Point(lon, lat);
        NearQuery nearQuery = NearQuery.near(userLocation)
                .maxDistance(new Distance(nearbyRadiusKm, Metrics.KILOMETERS))
                .inKilometers();

        return mongoTemplate.geoNear(nearQuery, Museum.class, "museums")
                .map(museumMapper::toGeoResponse)
                .skip(offset)
                .take(limit);
    }

    @Override
    public Mono<MuseumResponse> update(String id, MuseumUpdateRequest request) {
        String normalizedName = NameNormalizer.normalize(request.officialName());

        return museumRepository.existsByNormalizedOfficialNameAndIdNot(normalizedName, id)
                .flatMap(dup -> {
                    if (dup) {
                        return Mono.error(new DataIntegrityViolationException(
                                "Museum with name '" + request.officialName() + "' already exists."));
                    }
                    return museumRepository.findById(id)
                            .switchIfEmpty(Mono.error(new EntityNotFoundException("Museum not found: " + id)))
                            .flatMap(entity -> {
                                museumMapper.updateEntityFromRequest(entity, request);
                                entity.setNormalizedOfficialName(normalizedName);
                                entity.setOfficialName(request.officialName());
                                entity.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));

                                return museumRepository.save(entity);
                            })
                            .map(museumMapper::toResponse);
                });
    }
}
