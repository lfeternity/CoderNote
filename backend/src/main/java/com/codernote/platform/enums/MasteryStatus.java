package com.codernote.platform.enums;

import java.util.Arrays;

public enum MasteryStatus {
    NOT_MASTERED,
    REVIEWING,
    MASTERED;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(value));
    }
}
