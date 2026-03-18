package com.keyservice.service;

import com.keyservice.validator.KeySizeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class KeyService {

    private static final Logger log = LoggerFactory.getLogger(KeyService.class);

    private final KeySizeValidator validator = new KeySizeValidator();

    public record KeyResult(String shortSha, long durationMs) {}

    public KeyResult generate(int size) {
        if (!validator.isValid(size)) {
            log.warn("[KEY] rejected invalid size={}", size);
            throw new IllegalArgumentException(
                "Invalid key size: " + size + ". Allowed: 512, 1024, 2048, 4096"
            );
        }

        log.info("[KEY] generating RSA-{} ...", size);
        try {
            long start = System.currentTimeMillis();

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(size);
            byte[] publicKeyBytes = generator.generateKeyPair().getPublic().getEncoded();

            long durationMs = System.currentTimeMillis() - start;
            String shortSha = Base64.getEncoder()
                    .encodeToString(publicKeyBytes)
                    .substring(0, 8);

            log.info("[KEY] done size={} shortSha={} durationMs={}", size, shortSha, durationMs);
            return new KeyResult(shortSha, durationMs);

        } catch (NoSuchAlgorithmException e) {
            log.error("[KEY] RSA algorithm not available", e);
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }
}
