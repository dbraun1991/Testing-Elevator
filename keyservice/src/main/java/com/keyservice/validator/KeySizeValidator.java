package com.keyservice.validator;

import java.util.Set;

/**
 * Validates RSA key sizes accepted by the service.
 *
 * <p>This class is the sole mutation-testing target for Pitest (P1 stage).
 * Keeping the validation logic isolated here makes mutation scores easy to
 * interpret and avoids noise from surrounding controller or service code.</p>
 */
public class KeySizeValidator {

    /** RSA key sizes (in bits) that the service accepts. */
    private static final Set<Integer> ALLOWED_SIZES = Set.of(512, 1024, 2048, 4096);

    /**
     * Checks whether the given key size is accepted.
     *
     * @param size the RSA key size in bits
     * @return {@code true} if {@code size} is one of 512, 1024, 2048, or 4096;
     *         {@code false} otherwise
     */
    public boolean isValid(int size) {
        return ALLOWED_SIZES.contains(size);
    }
}