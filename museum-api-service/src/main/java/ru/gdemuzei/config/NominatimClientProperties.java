package ru.gdemuzei.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import java.time.Duration;

@ConfigurationProperties(prefix = "clients.nominatim")
public record NominatimClientProperties(
        String baseUrl,
        String userAgent,
        Integer connectTimeoutMs,
        Duration responseTimeout,
        DataSize maxInMemoryBytes,
        Integer maxConnections
) {}