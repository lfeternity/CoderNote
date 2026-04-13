package com.codernote.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.oauth")
public class OauthProperties {

    private String frontendBaseUrl = "http://127.0.0.1:5173";

    private String backendBaseUrl = "";

    private Integer stateExpireSeconds = 300;

    private Integer pendingExpireSeconds = 600;

    private Map<String, PlatformConfig> platforms = new HashMap<>();

    public PlatformConfig findPlatformConfig(String platformCode) {
        if (platformCode == null) {
            return null;
        }
        return platforms.get(platformCode.trim().toLowerCase(Locale.ROOT));
    }

    @Data
    public static class PlatformConfig {
        private Boolean enabled = Boolean.FALSE;
        private String clientId = "";
        private String clientSecret = "";
    }
}
