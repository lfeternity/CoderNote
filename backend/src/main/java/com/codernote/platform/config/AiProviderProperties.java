package com.codernote.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    private String defaultModel = "SAFE_GPT_SIM";

    private Integer timeoutMs = 20000;

    private Boolean allowRuntimeModelConfig = Boolean.FALSE;

    private List<String> runtimeAllowedHosts = Arrays.asList(
            "api.openai.com",
            "dashscope.aliyuncs.com",
            "api.moonshot.cn",
            "api.deepseek.com",
            "generativelanguage.googleapis.com",
            "api.anthropic.com"
    );

    private Provider qwen = new Provider();

    private Provider kimi = new Provider();

    private Provider openai = new Provider();

    private Provider deepseek = new Provider();

    private Provider gemini = new Provider();

    private Provider claude = new Provider();

    @Data
    public static class Provider {
        private String apiKey;
        private String endpoint;
        private String model;
    }
}
