package ru.gdemuzei.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.gdemuzei.models.Museum;

public interface MuseumRepository extends ReactiveMongoRepository<Museum, String> {

    Mono<Boolean> existsByNormalizedOfficialName(String normalizedOfficialName);

    Flux<Museum> findByOsmDataIsNull();

    Flux<Museum> findAllBy(Pageable pageable);

    Mono<Boolean> existsByNormalizedOfficialNameAndIdNot(String normalizedOfficialName, String id);
}