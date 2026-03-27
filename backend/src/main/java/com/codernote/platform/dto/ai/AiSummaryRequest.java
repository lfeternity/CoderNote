package com.codernote.platform.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiSummaryRequest {

    private String targetType;

    private Long targetId;

    private String title;

    private String content;

    private String language;

    private List<String> tagNames;

    private String summaryType;

    private String model;

    private AiModelConfig modelConfig;
}
