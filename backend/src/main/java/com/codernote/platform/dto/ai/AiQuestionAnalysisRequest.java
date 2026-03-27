package com.codernote.platform.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiQuestionAnalysisRequest {

    private Long questionId;

    private String errorCode;

    private String exceptionMessage;

    private String contextDescription;

    private String language;

    private List<String> tagNames;

    private String model;

    private AiModelConfig modelConfig;
}
