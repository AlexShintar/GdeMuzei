package ru.gdemuzei.bot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gdemuzei.contracts.MuseumSummaryDto;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, MuseumSummaryDto> caffeineCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(1500)
                .expireAfterAccess(Duration.ofDays(7))
                .scheduler(Scheduler.systemScheduler())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.info("Museum " + key + " evicted: " + cause))
                .build();
    }
}
