package ru.gdemuzei.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;

import reactor.netty.http.client.HttpClient;
import java.util.List;

@Configuration
@EnableConfigurationProperties(NominatimClientProperties.class)
//@Profile("shell")
public class NominatimClientConfig {

    @Bean("nominatimWebClient")
    public WebClient nominatimWebClient(WebClient.Builder builder, NominatimClientProperties p) {

        ConnectionProvider pool = ConnectionProvider.builder("nominatim-pool")
                .maxConnections(p.maxConnections())
                .build();

        HttpClient http = HttpClient.create(pool)
                .compress(true)
                .responseTimeout(p.responseTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, p.connectTimeoutMs());

        return builder
                .baseUrl(p.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeaders(h -> {
                    h.set(HttpHeaders.USER_AGENT, p.userAgent());
                    h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .codecs(c -> c.defaultCodecs().maxInMemorySize((int) p.maxInMemoryBytes().toBytes()))
                .build();
    }
}