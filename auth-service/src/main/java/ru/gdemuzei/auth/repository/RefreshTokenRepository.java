package ru.gdemuzei.auth.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {

    Mono<RefreshToken> findByToken(String token);

    @Query("{'family': ?0}")
    Mono<Long> deleteByFamily(String family);
}
