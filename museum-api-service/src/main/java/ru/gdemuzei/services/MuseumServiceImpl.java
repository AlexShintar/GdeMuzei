package ru.gdemuzei.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.contracts.MuseumDto;
import ru.gdemuzei.dto.*;
import ru.gdemuzei.exceptions.EntityNotFoundException;
import ru.gdemuzei.mapper.MuseumMapper;
import ru.gdemuzei.models.Museum;
import ru.gdemuzei.models.OsmData;
import ru.gdemuzei.repositories.MuseumRepository;
import ru.gdemuzei.util.NameNormalizer;

import java.time.Instant;

@Service
@Slf4j
public class MuseumServiceImpl implements MuseumService {
    private final MuseumMapper museumMapper;

    private final MuseumRepository museumRepository;

    private final ReactiveMongoTemplate mongoTemplate;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final double nearbyRadiusKm;

    public MuseumServiceImpl(MuseumMapper museumMapper,
                             MuseumRepository museumRepository,
                             ReactiveMongoTemplate mongoTemplate, ReactiveRedisTemplate<String, String> redisTemplate,
                             @Value("${application.nearby.radius-km:10.0}") double nearbyRadiusKm) {
        this.museumMapper = museumMapper;
        this.museumRepository = museumRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.nearbyRadiusKm = nearbyRadiusKm;
    }

    /**
     * Поиск музея по идентификатору.
     */
    @Override
    public Mono<MuseumDto> findById(String id) {
        return museumRepository.findById(id)
                .map(museumMapper::toDto)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Museum not found: " + id)));
    }

    /**
     * Возвращает пагинированный список всех музеев для администратора.
     * Показывает как активные, так и удалённые музеи.
     */
    @Override
    public Mono<Page<MuseumSummaryDto>> findAllForAdmin(PageRequest pageRequest) {
        Flux<MuseumSummaryDto> content = museumRepository.findAllBy(pageRequest)
                .map(museumMapper::toSummaryDto);
        return content.collectList()
                .zipWith(countAll())
                .map(tuple -> new PageImpl<>(
                        tuple.getT1(),
                        pageRequest,
                        tuple.getT2()
                ));
    }

    /**
     * Общее количество музеев.
     */
    @Override
    public Mono<Long> countAll() {
        return museumRepository.count();
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

    /**
     * Музеи, у которых отсутствуют OSM-данные.
     */
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

    /**
     * Помечает музей как удалённый (soft delete).
     * Инвалидирует кэш summary в Redis.
     */
    @Override
    public Mono<Void> markAsDeleted(String id) {
        return museumRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Museum not found: " + id)))
                .flatMap(museum -> {
                    museum.setDeleted(true);
                    museum.setDeletedAt(Instant.now());
                    return museumRepository.save(museum);
                })
                .flatMap(deleted ->
                        redisTemplate.delete("museum:" + id + ":summary")
                                .doOnSuccess(count -> log.info("Cache invalidated for deleted museum:{}", id))
                )
                .then();
    }
}
