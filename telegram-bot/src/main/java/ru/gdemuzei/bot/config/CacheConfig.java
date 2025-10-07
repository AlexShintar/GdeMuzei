package ru.gdemuzei.bot.config;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gdemuzei.bot.dto.MuseumSummaryDto;

@Slf4j
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, MuseumSummaryDto> museumCache() {
        return com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(1500)
                .expireAfterAccess(java.time.Duration.ofDays(7))
                .scheduler(com.github.benmanes.caffeine.cache.Scheduler.systemScheduler())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.info("Museum " + key + " evicted: " + cause))
                .build();
    }
}
