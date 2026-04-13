package com.codernote.platform.dto.ai;

import lombok.Data;

@Data
public class AiModelConfig {

    /**
     * Provider key, compatible with OPENAI/QWEN/KIMI/DEEPSEEK/GEMINI/CLAUDE (case-insensitive).
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
     * Actual provider model id, for example gpt-5.2 / qwen-plus / deepseek-chat / gemini-2.0-flash / claude-3-5-sonnet-latest.
     */
    private String modelName;
}
