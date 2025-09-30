package ru.gdemuzei.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@EnableConfigurationProperties(MuseumApiProperties.class)
public class MuseumApiConfig {

    @Bean("apiModuleWebClient")
    public WebClient apiModuleWebClient(WebClient.Builder builder, MuseumApiProperties p) {
        ConnectionProvider pool = ConnectionProvider.builder("museum-api-pool")
                .maxConnections(p.maxConnections())
                .build();

        HttpClient http = HttpClient.create(pool)
                .compress(true)
                .responseTimeout(p.responseTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, p.connectTimeoutMs());

        return builder
                .baseUrl(p.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .codecs(c -> c.defaultCodecs().maxInMemorySize((int) p.maxInMemory().toBytes()))
                .build();
    }
}
