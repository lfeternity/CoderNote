package com.codernote.platform.dto.ai;

import lombok.Data;

@Data
public class AiChatVO {

    private String model;

    private Integer usedCount;

    private Integer remainingCount;

    private String answerMarkdown;
}
