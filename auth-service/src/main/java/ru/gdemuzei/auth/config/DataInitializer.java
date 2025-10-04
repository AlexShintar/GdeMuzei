package ru.gdemuzei.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.model.User;
import ru.gdemuzei.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeAdminUsers().block();
    }

    private Mono<Void> initializeAdminUsers() {
        return createUserIfNotExists("admin1", "admin123", List.of("ROLE_ADMIN"))
                .then(createUserIfNotExists("admin2", "admin456", List.of("ROLE_ADMIN")))
                .doOnSuccess(v -> log.info("Admin users initialized"))
                .then();
    }

    private Mono<User> createUserIfNotExists(String username, String password, List<String> authorities) {
        return userRepository.existsByUsername(username)
                .flatMap(exists -> {
                    if (!exists) {
                        User user = User.builder()
                                .username(username)
                                .password(passwordEncoder.encode(password))
                                .authorities(authorities)
                                .enabled(true)
                                .createdAt(LocalDateTime.now())
                                .build();

                        return userRepository.save(user)
                                .doOnSuccess(u -> log.info("Created user: {}", username));
                    }
                    return Mono.empty();
                });
    }
}
