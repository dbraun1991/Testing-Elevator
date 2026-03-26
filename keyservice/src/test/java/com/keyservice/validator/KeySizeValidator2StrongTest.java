package com.keyservice.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.junit.jupiter.api.Disabled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Code#3
 */
@Disabled()
class KeySizeValidator2StrongTest {

    private final KeySizeValidator validator = new KeySizeValidator();

    // ===========   Erlaubte Werte   ===========

    @ParameterizedTest(name = "size {0} is valid")
    @ValueSource(ints = {
            512,
            1024,
            2048,
            4096
    })
    void allowedSizesAreValid(int size) {
        // Prämisse / Setup (Arrange)
        int input = size;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isTrue();
    }

    // ===========   Ungültige Werte   ===========

    @ParameterizedTest(name = "size {0} is invalid")
    @ValueSource(ints = {
            -1,
            0,
            1,
            511,   // boundary: direkt unter 512
            513,   // boundary: direkt über 512
            1023,  // boundary: direkt unter 1024
            1025,  // boundary: direkt über 1024
            2047,  // boundary: direkt unter 2048
            2049,  // boundary: direkt über 2048
            4095,  // boundary: direkt unter 4096
            4097,  // boundary: direkt über 4096
            8192,
            Integer.MAX_VALUE
    })
    void invalidSizesAreRejected(int size) {
        // Prämisse / Setup (Arrange)
        int input = size;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isFalse();
    }

    // ===========   Explizite Einzeltests für Lesbarkeit in der Demo   ===========

    @Test
    void zero_isInvalid() {
        // Prämisse / Setup (Arrange)
        int input = 0;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isFalse();
    }

    @Test
    void negativeValue_isInvalid() {
        // Prämisse / Setup (Arrange)
        int input = -1;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isFalse();
    }

    @Test
    void arbitraryLargeValue_isInvalid() {
        // Prämisse / Setup (Arrange)
        int input = 99999;

        // Execute (Act)
        boolean result = validator.isValid(input);

        // Assertion / Comparison (Assert)
        assertThat(result).isFalse();
    }
}
