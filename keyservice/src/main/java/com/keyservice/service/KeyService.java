package com.keyservice.service;

import com.keyservice.validator.KeySizeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service backing the {@code GET /key} endpoint.
 *
 * <p>Validates the requested key size via {@link KeySizeValidator}, then generates
 * an RSA key pair and measures wall-clock generation time. At {@code size=4096}
 * the generation is intentionally expensive — under sustained load this causes
 * the thread-pool saturation that is demonstrated in the P4 load-test stage.</p>
 */
@Service
public class KeyService {

    private static final Logger log = LoggerFactory.getLogger(KeyService.class);

    private final KeySizeValidator validator = new KeySizeValidator();

    /**
     * Holds the result of a single RSA key-generation request.
     *
     * @param shortSha    the first 8 characters of the Base64-encoded public key,
     *                    used as a human-readable fingerprint in responses and logs
     * @param durationMs  wall-clock time in milliseconds taken to generate the key pair;
     *                    the primary metric observed during load tests
     */
    public record KeyResult(String shortSha, long durationMs) {}

    /**
     * Generates an RSA key pair of the requested size and returns a short fingerprint
     * together with the generation duration.
     *
     * @param size the RSA key size in bits; must be one of 512, 1024, 2048, or 4096
     * @return a {@link KeyResult} containing the public-key fingerprint and generation time
     * @throws IllegalArgumentException if {@code size} is not an allowed value
     * @throws IllegalStateException    if the RSA algorithm is unexpectedly unavailable in this JVM
     */
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
