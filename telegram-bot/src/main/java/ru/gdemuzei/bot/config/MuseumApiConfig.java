package ru.gdemuzei.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@ConfigurationProperties(prefix = "museum.api")
@Getter
@Setter
public class MuseumApiConfig {

    private String baseUrl;

    private String searchPath;

    private int timeoutMs = 10000;

    private int maxInMemoryKb = 256;

    @Bean
    public WebClient museumWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("museum-pool")
                .maxConnections(50)
                .pendingAcquireMaxCount(100)
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(java.time.Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(maxInMemoryKb * 1024))
                        .build())
                .build();
    }

    public String searchPath() {
        return searchPath;
    }
}
