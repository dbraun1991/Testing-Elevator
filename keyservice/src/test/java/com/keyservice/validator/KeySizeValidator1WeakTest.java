package com.keyservice.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Code#1
 */
class KeySizeValidator1WeakTest {

    private final KeySizeValidator validator = new KeySizeValidator();

    @Test
    void size512IsValid() {
        // Prämisse / Setup (Arrange)
        int input = 512;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isTrue();
    }

    @Test
    void size4096IsValid() {
        // Prämisse / Setup (Arrange)
        int input = 4096;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isTrue();
    }
}
