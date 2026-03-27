package com.codernote.platform.enums;

import java.util.Arrays;

public enum ReviewContentType {
    QUESTION,
    NOTE;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(value));
    }
}
