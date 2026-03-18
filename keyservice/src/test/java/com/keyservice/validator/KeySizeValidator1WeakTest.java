package com.keyservice.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1 — Demo-Start (schwache Tests)
 *
 * Nur zwei erlaubte Werte werden geprüft.
 * Kein Test für ungültige Werte, keine Boundaries.
 *
 * → Pitest wird zeigen: ein Großteil der Mutationen überlebt.
 *
 * Ausführen:
 *   mvn test -Dtest=KeySizeValidatorWeakTest
 *   mvn test-compile org.pitest:pitest-maven:mutationCoverage
 */
class KeySizeValidator1WeakTest {

    private final KeySizeValidator validator = new KeySizeValidator();

    @Test
    void size512IsValid() {
        assertThat(validator.isValid(512)).isTrue();
    }

    @Test
    void size4096IsValid() {
        assertThat(validator.isValid(4096)).isTrue();
    }
}