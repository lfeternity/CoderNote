package com.codernote.platform.enums;

import org.springframework.util.StringUtils;

import java.util.Locale;

public enum OauthPlatform {
    QQ("qq", "QQ", "#12B7F5"),
    WECHAT("wechat", "微信", "#07C160"),
    GITHUB("github", "GitHub", "#24292F");

    private final String code;
    private final String displayName;
    private final String brandColor;

    OauthPlatform(String code, String displayName, String brandColor) {
        this.code = code;
        this.displayName = displayName;
        this.brandColor = brandColor;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public static OauthPlatform fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (OauthPlatform value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
