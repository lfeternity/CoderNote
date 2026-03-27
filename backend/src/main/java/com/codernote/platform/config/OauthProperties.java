package com.codernote.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.oauth")
public class OauthProperties {

    private String frontendBaseUrl = "http://127.0.0.1:5173";

    private Integer stateExpireSeconds = 300;

    private Integer codeExpireSeconds = 300;

    private Integer pendingExpireSeconds = 600;
}
