package ru.gdemuzei.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.gdemuzei.auth.util.JwtUtil;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class JwksController {

    private final JwtUtil jwtUtil;

    @GetMapping("/jwks.json")
    public Mono<Map<String, Object>> jwks() {
        RSAPublicKey publicKey = jwtUtil.getPublicKey();

        Map<String, Object> key = new HashMap<>();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("kid", jwtUtil.getKeyId());
        key.put("n", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray()));
        key.put("e", Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getPublicExponent().toByteArray()));

        return Mono.just(Map.of("keys", List.of(key)));
    }
}
