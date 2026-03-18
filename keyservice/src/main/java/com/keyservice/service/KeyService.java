package com.keyservice.service;

import com.keyservice.validator.KeySizeValidator;
import org.springframework.stereotype.Service;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class KeyService {

    private final KeySizeValidator validator = new KeySizeValidator();

    public record KeyResult(String shortSha, long durationMs) {}

    public KeyResult generate(int size) {
        if (!validator.isValid(size)) {
            throw new IllegalArgumentException(
                "Invalid key size: " + size + ". Allowed: 512, 1024, 2048, 4096"
            );
        }

        try {
            long start = System.currentTimeMillis();

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(size);
            byte[] publicKeyBytes = generator.generateKeyPair().getPublic().getEncoded();

            long durationMs = System.currentTimeMillis() - start;
            String shortSha = Base64.getEncoder()
                    .encodeToString(publicKeyBytes)
                    .substring(0, 8);

            return new KeyResult(shortSha, durationMs);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }
}