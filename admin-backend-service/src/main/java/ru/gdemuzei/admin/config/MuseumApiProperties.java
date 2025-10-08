package ru.gdemuzei.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "clients.museum-api")
public record MuseumApiProperties(
        String baseUrl,
        Integer connectTimeoutMs,
        Duration responseTimeout,
        DataSize maxInMemory,
        Integer maxConnections
) {}