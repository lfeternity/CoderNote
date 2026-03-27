package com.codernote.platform.util;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashPassword(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt());
    }

    public static boolean matchesPassword(String raw, String storedHash) {
        if (raw == null || !StringUtils.hasText(storedHash)) {
            return false;
        }
        if (isBcryptHash(storedHash)) {
            try {
                return BCrypt.checkpw(raw, storedHash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return md5(raw).equals(storedHash);
    }

    public static boolean needsUpgrade(String storedHash) {
        return StringUtils.hasText(storedHash) && !isBcryptHash(storedHash);
    }

    private static boolean isBcryptHash(String value) {
        return value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$");
    }
}
