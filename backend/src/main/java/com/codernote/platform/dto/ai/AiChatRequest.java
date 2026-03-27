package com.codernote.platform.dto.ai;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class AiChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private List<ChatMessage> history;

    private List<String> contextTags;

    private String model;

    private AiModelConfig modelConfig;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
