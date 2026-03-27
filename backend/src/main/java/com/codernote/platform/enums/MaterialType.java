package com.codernote.platform.enums;

import java.util.Arrays;

public enum MaterialType {
    KNOWLEDGE_NOTE,
    SOLUTION_TUTORIAL,
    VIDEO_LINK,
    CODE_TEMPLATE,
    DOC_LINK;

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(v -> v.name().equals(value));
    }
}
