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
        // Arrange
        int input = 512;

        // Act
        boolean result = validator.isValid(input);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void size4096IsValid() {
        // Arrange
        int input = 4096;

        // Act
        boolean result = validator.isValid(input);

        // Assert
        assertThat(result).isTrue();
    }
}
