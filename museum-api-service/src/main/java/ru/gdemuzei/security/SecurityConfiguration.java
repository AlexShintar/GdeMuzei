package ru.gdemuzei.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    /**
     * Конфигурация Security для production.
     * - Публичные эндпоинты (/api/public/**) доступны всем
     * - Админские эндпоинты (/api/admin/**) требуют аутентификации
     */
    @Bean
    @Profile("!test") // Не применяется для профиля test
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Отключаем CSRF для REST API
                .authorizeExchange(exchange -> exchange
                        // Публичные эндпоинты - доступны всем
                        .pathMatchers("/api/public/**").permitAll()

                        // Админские эндпоинты - требуют аутентификации
                        .pathMatchers("/api/admin/**").authenticated()

                        // Все остальное - требует аутентификации
                        .anyExchange().authenticated()
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // Отключаем Basic Auth
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // Отключаем Form Login
                .build();
    }

    /**
     * Конфигурация Security для тестирования.
     * Разрешает все запросы без аутентификации.
     */
    @Bean
    @Profile("test") // Применяется только для профиля test
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll() // Все запросы разрешены
                )
                .build();
    }
}
