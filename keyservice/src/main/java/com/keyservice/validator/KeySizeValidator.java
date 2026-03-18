package com.keyservice.validator;

import java.util.Set;

public class KeySizeValidator {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(512, 1024, 2048, 4096);

    public boolean isValid(int size) {
        return ALLOWED_SIZES.contains(size);
    }
}