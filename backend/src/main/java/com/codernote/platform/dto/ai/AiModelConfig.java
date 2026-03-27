package com.codernote.platform.dto.ai;

import lombok.Data;

@Data
public class AiModelConfig {

    /**
     * Provider key, compatible with OPENAI/QWEN/KIMI (case-insensitive).
     */
    private String provider;

    /**
     * Compatible API base url or full completion endpoint.
     */
    private String baseUrl;

    /**
     * Provider API key, for example sk-xxx.
     */
    private String apiKey;

    /**
     * Actual provider model id, for example gpt-5.2 / qwen-plus / moonshot-v1-8k.
     */
    private String modelName;
}
