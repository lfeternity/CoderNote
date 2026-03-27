package com.codernote.platform.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiQuestionAnalysisVO {

    private String model;

    private Integer usedCount;

    private Integer remainingCount;

    private String markdown;

    private String errorLocation;

    private String exceptionType;

    private String coreReason;

    private String principle;

    private String fixedCode;

    private String solutionText;

    private List<String> relatedSuggestions;
}
